package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.request.UserUpdateRequest
import com.ampnet.crowdfundingbackend.controller.pojo.response.OrganizationInvitesListResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.UserResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.UsersListResponse
import com.ampnet.crowdfundingbackend.enums.OrganizationRoleType
import com.ampnet.crowdfundingbackend.enums.PrivilegeType
import com.ampnet.crowdfundingbackend.enums.UserRoleType
import com.ampnet.crowdfundingbackend.enums.AuthMethod
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.OrganizationInvite
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.persistence.repository.OrganizationInviteRepository
import com.ampnet.crowdfundingbackend.persistence.repository.OrganizationRepository
import com.ampnet.crowdfundingbackend.persistence.repository.RoleRepository
import com.ampnet.crowdfundingbackend.security.WithMockCrowdfoundUser
import com.ampnet.crowdfundingbackend.service.UserService
import com.ampnet.crowdfundingbackend.service.pojo.CreateUserServiceRequest
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.ZonedDateTime

class UserControllerTest : ControllerTestBase() {

    private val pathUsers = "/users"
    private val pathMe = "/me"

    @Autowired
    private lateinit var userService: UserService
    @Autowired
    private lateinit var organizationRepository: OrganizationRepository
    @Autowired
    private lateinit var organizationInviteRepository: OrganizationInviteRepository
    @Autowired
    private lateinit var roleRepository: RoleRepository

    private lateinit var testUser: TestUser
    private lateinit var testContext: TestContext

    @BeforeEach
    fun initTestData() {
        testUser = TestUser()
        testContext = TestContext()
    }

    @Test
    @WithMockCrowdfoundUser(email = "test@test.com", privileges = [PrivilegeType.PRO_PROFILE])
    fun mustBeAbleToGetOwnProfile() {
        suppose("User exists in database") {
            databaseCleanerService.deleteAllUsers()
            testUser.email = "test@test.com"
            saveTestUser()
        }

        verify("The controller must return user data") {
            val result = mockMvc.perform(get(pathMe))
                    .andExpect(status().isOk)
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andReturn()
            val userResponse: UserResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(userResponse.email).isEqualTo(testUser.email)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_PROFILE])
    fun mustBeAbleToGetAListOfUsers() {
        suppose("Some user exists in database") {
            databaseCleanerService.deleteAllUsers()
            saveTestUser()
        }

        verify("The controller returns a list of users") {
            val result = mockMvc.perform(get(pathUsers))
                    .andExpect(status().isOk)
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andReturn()

            val listResponse: UsersListResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(listResponse.users).hasSize(1)
        }
    }

    @Test
    @WithMockCrowdfoundUser(role = UserRoleType.USER)
    fun mustNotBeAbleToGetAListOfUsersWithoutAdminPermission() {
        verify("The user with role USER cannot fetch a list of users") {
            mockMvc.perform(get(pathUsers))
                    .andExpect(status().isForbidden)
        }
    }

    @Test
    @WithMockCrowdfoundUser(completeProfile = false, email = "test@test.com")
    fun mustEnableFetchingOwnProfileForIncompleteUserProfile() {
        suppose("User with incomplete profile exists in database") {
            databaseCleanerService.deleteAllUsers()
            val user = CreateUserServiceRequest("test@test.com", null, null, null, null, null, AuthMethod.EMAIL)
            userService.create(user)
        }

        verify("The system returns user profile") {
            val result = mockMvc.perform(get(pathMe))
                    .andExpect(status().isOk)
                    .andReturn()

            val userResponse: UserResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(userResponse.email).isEqualTo("test@test.com")
            assertThat(userResponse.firstName).isNull()
            assertThat(userResponse.lastName).isNull()
            assertThat(userResponse.country).isNull()
            assertThat(userResponse.phoneNumber).isNull()
        }
    }

    @Test
    @WithMockCrowdfoundUser(completeProfile = false, role = UserRoleType.ADMIN)
    fun mustThrowErrorForIncompleteUserProfile() {
        verify("User with incomplete profile with get an error") {
            mockMvc.perform(get(pathUsers))
                    .andExpect(status().isConflict)
        }
    }

    @Test
    @WithMockCrowdfoundUser(completeProfile = false, email = "john@smith.com")
    fun mustBeAbleToUpdateProfile() {
        suppose("User exists in database") {
            databaseCleanerService.deleteAllUsers()
            saveTestUser()
        }

        verify("User can update his profile") {
            testUser.firstName = "NewFirstName"
            testUser.phoneNumber = "099123123"
            val request = getUpdateUserRequestFromTestUser()
            val result = mockMvc.perform(
                    post(pathMe)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk)
                    .andReturn()

            val userResponse: UserResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(userResponse.phoneNumber).isEqualTo(testUser.phoneNumber)
            assertThat(userResponse.firstName).isEqualTo(testUser.firstName)
            assertThat(userResponse.email).isEqualTo(testUser.email)
        }
        verify("User profile is updated in database") {
            val user = userService.find(testUser.email)
            assertThat(testUser).isNotNull
            assertThat(user!!.firstName).isEqualTo(testUser.firstName)
            assertThat(user.phoneNumber).isEqualTo(testUser.phoneNumber)
        }
    }

    @Test
    @WithMockCrowdfoundUser(email = "other@user.com")
    fun mustNotBeAbleToUpdateOthersUserProfile() {
        verify("User cannot update others profile") {
            val request = getUpdateUserRequestFromTestUser()
            mockMvc.perform(
                    post(pathMe)
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden)
        }
    }

    @Test
    @WithMockCrowdfoundUser(email = "john@smith.com", privileges = [PrivilegeType.PRO_ORG_INVITE])
    fun mustBeAbleToGetOrganizationInvites() {
        suppose("User exists in database") {
            databaseCleanerService.deleteAllUsers()
            testContext.user = saveTestUser()
        }
        suppose("User has organization invites") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("Test org", testContext.user)
            testContext.invitedByUser = createUser("invited@by.com")
            createOrganizationInvite(testContext.user.id, testContext.organization.id, testContext.invitedByUser.id,
                    OrganizationRoleType.ORG_MEMBER)
        }

        verify("User can get a list of his invites") {
            val result = mockMvc.perform(get("$pathMe/invites"))
                    .andExpect(status().isOk)
                    .andReturn()

            val invitesResponse: OrganizationInvitesListResponse =
                    objectMapper.readValue(result.response.contentAsString)
            assertThat(invitesResponse.organizationInvites).hasSize(1)
            val invite = invitesResponse.organizationInvites.first()
            assertThat(invite.role).isEqualTo(OrganizationRoleType.ORG_MEMBER)
            assertThat(invite.organizationId).isEqualTo(testContext.organization.id)
            assertThat(invite.organizationName).isEqualTo(testContext.organization.name)
            assertThat(invite.invitedByUser).isEqualTo(testContext.invitedByUser.getFullName())
        }
    }

    @Test
    @WithMockCrowdfoundUser(email = "john@smith.com", privileges = [PrivilegeType.PWO_ORG_INVITE])
    fun mustBeAbleToAcceptOrganizationInvite() {
        suppose("User exists in database") {
            databaseCleanerService.deleteAllUsers()
            testContext.user = saveTestUser()
        }
        suppose("User has organization invites") {
            databaseCleanerService.deleteAllOrganizations()
            databaseCleanerService.deleteAllOrganizationInvites()
            testContext.organization = createOrganization("Test org", testContext.user)
            testContext.invitedByUser = createUser("invited@by.com")
            createOrganizationInvite(testContext.user.id, testContext.organization.id, testContext.invitedByUser.id,
                    OrganizationRoleType.ORG_MEMBER)
        }

        verify("User can get a list of his invites") {
            mockMvc.perform(post("$pathMe/invites/${testContext.organization.id}/accept"))
                    .andExpect(status().isOk)
                    .andReturn()
        }
        verify("User is a member of organization") {
            val organizations = organizationRepository.findAllOrganizationsForUser(testContext.user.id)
            assertThat(organizations).hasSize(1)
            val organization = organizations.first()
            assertThat(organization.id).isEqualTo(testContext.organization.id)
            assertThat(organization.memberships).hasSize(1)
            assertThat(organization.memberships!!.first().role.id).isEqualTo(OrganizationRoleType.ORG_MEMBER.id)
        }
        verify("User invitation is deleted") {
            val optionalInvite = organizationInviteRepository
                    .findByOrganizationIdAndUserId(testContext.organization.id, testContext.user.id)
            assertThat(optionalInvite).isNotPresent
        }
    }

    @Test
    @WithMockCrowdfoundUser(email = "john@smith.com", privileges = [PrivilegeType.PWO_ORG_INVITE])
    fun mustBeAbleToRejectOrganizationInvite() {
        suppose("User exists in database") {
            databaseCleanerService.deleteAllUsers()
            testContext.user = saveTestUser()
        }
        suppose("User has organization invites") {
            databaseCleanerService.deleteAllOrganizations()
            databaseCleanerService.deleteAllOrganizationInvites()
            testContext.organization = createOrganization("Test org", testContext.user)
            testContext.invitedByUser = createUser("invited@by.com")
            createOrganizationInvite(testContext.user.id, testContext.organization.id, testContext.invitedByUser.id,
                    OrganizationRoleType.ORG_MEMBER)
        }

        verify("User can get a list of his invites") {
            mockMvc.perform(post("$pathMe/invites/${testContext.organization.id}/reject"))
                    .andExpect(status().isOk)
                    .andReturn()
        }
        verify("User is not a member of organization") {
            val organizations = organizationRepository.findAllOrganizationsForUser(testContext.user.id)
            assertThat(organizations).hasSize(0)
        }
        verify("User invitation is deleted") {
            val optionalInvite = organizationInviteRepository
                    .findByOrganizationIdAndUserId(testContext.organization.id, testContext.user.id)
            assertThat(optionalInvite).isNotPresent
        }
    }

    @Test
    @WithMockCrowdfoundUser(email = "john@smith.com", privileges = [PrivilegeType.PRA_PROFILE])
    fun adminMustBeAbleToGetUserWithId() {
        suppose("User exists in database") {
            databaseCleanerService.deleteAllUsers()
            testContext.user = saveTestUser()
        }

        verify("User with PRA_PROFILE privilege can get user via id") {
            val result = mockMvc.perform(get("$pathUsers/${testContext.user.id}"))
                    .andExpect(status().isOk)
                    .andReturn()

            val userResponse: UserResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(userResponse.email).isEqualTo(testContext.user.email)
        }
    }

    @Test
    @WithMockCrowdfoundUser(email = "non-existing@user.com", privileges = [PrivilegeType.PRO_PROFILE])
    fun mustThrowExceptionIfUserDoesNotExists() {
        suppose("User is not stored in database") {
            databaseCleanerService.deleteAllUsers()
        }

        verify("Controller will throw exception for non existing user on /me path") {
            mockMvc.perform(get(pathMe))
                    .andExpect(status().isNotFound)
        }
    }

    private fun saveTestUser(): User {
        return createUser(testUser.email)
    }

    private fun createUser(email: String): User {
        val request = CreateUserServiceRequest(
                email = email,
                password = testUser.password,
                firstName = testUser.firstName,
                lastName = testUser.lastName,
                countryId = testUser.countryId,
                phoneNumber = testUser.phoneNumber,
                authMethod = testUser.authMethod
        )
        return userService.create(request)
    }

    private fun createOrganization(name: String, createdBy: User): Organization {
        val organization = Organization::class.java.newInstance()
        organization.name = name
        organization.legalInfo = "Some info"
        organization.createdAt = ZonedDateTime.now()
        organization.approved = true
        organization.createdByUser = createdBy
        organization.documents = emptyList()
        return organizationRepository.save(organization)
    }

    private fun createOrganizationInvite(userId: Int, organizationId: Int, invitedBy: Int, role: OrganizationRoleType): OrganizationInvite {
        val organizationInvite = OrganizationInvite::class.java.newInstance()
        organizationInvite.userId = userId
        organizationInvite.organizationId = organizationId
        organizationInvite.invitedBy = invitedBy
        organizationInvite.role = roleRepository.getOne(role.id)
        organizationInvite.createdAt = ZonedDateTime.now()
        return organizationInviteRepository.save(organizationInvite)
    }

    private fun getUpdateUserRequestFromTestUser(): UserUpdateRequest {
        return UserUpdateRequest(
                testUser.email,
                testUser.firstName,
                testUser.lastName,
                testUser.countryId,
                testUser.phoneNumber
        )
    }

    private class TestUser {
        var email = "john@smith.com"
        var password = "Password157!"
        var firstName = "John"
        var lastName = "Smith"
        var countryId = 1
        var phoneNumber = "0951234567"
        var authMethod = AuthMethod.EMAIL
    }

    private class TestContext {
        lateinit var user: User
        lateinit var organization: Organization
        lateinit var invitedByUser: User
    }
}
