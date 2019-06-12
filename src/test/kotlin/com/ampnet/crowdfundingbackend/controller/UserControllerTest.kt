package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.response.OrganizationInvitesListResponse
import com.ampnet.crowdfundingbackend.enums.OrganizationRoleType
import com.ampnet.crowdfundingbackend.enums.PrivilegeType
import com.ampnet.crowdfundingbackend.enums.UserRoleType
import com.ampnet.crowdfundingbackend.enums.AuthMethod
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.OrganizationInvite
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.persistence.repository.OrganizationInviteRepository
import com.ampnet.crowdfundingbackend.security.WithMockCrowdfoundUser
import com.ampnet.crowdfundingbackend.service.UserService
import com.ampnet.crowdfundingbackend.service.pojo.CreateUserServiceRequest
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.ZonedDateTime

class UserControllerTest : ControllerTestBase() {

    private val pathUsers = "/users"
    private val pathMe = "/me"

    @Autowired
    private lateinit var userService: UserService
    @Autowired
    private lateinit var organizationInviteRepository: OrganizationInviteRepository

    private lateinit var testUser: TestUser
    private lateinit var testContext: TestContext

    @BeforeEach
    fun initTestData() {
        testUser = TestUser()
        testContext = TestContext()
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
    @WithMockCrowdfoundUser(email = "john@smith.com", privileges = [PrivilegeType.PRO_ORG_INVITE])
    fun mustBeAbleToGetOrganizationInvites() {
        suppose("User exists in database") {
            databaseCleanerService.deleteAllUsers()
            testContext.user = saveTestUser()
        }
        suppose("User has organization invites") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("Test org", testContext.user)
            testContext.invitedByUser = createServiceUser("invited@by.com")
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
            testContext.invitedByUser = createServiceUser("invited@by.com")
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
            val memberships = organization.memberships ?: fail("Organization membership must no be null")
            assertThat(memberships).hasSize(1)
            assertThat(memberships.first().role.id).isEqualTo(OrganizationRoleType.ORG_MEMBER.id)
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
            testContext.invitedByUser = createServiceUser("invited@by.com")
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

    private fun saveTestUser(): User {
        return createServiceUser(testUser.email)
    }

    private fun createServiceUser(email: String): User {
        val request = CreateUserServiceRequest(
                email = email,
                password = testUser.password,
                firstName = testUser.firstName,
                lastName = testUser.lastName,
                phoneNumber = testUser.phoneNumber,
                authMethod = testUser.authMethod
        )
        return userService.create(request)
    }

    private fun createOrganizationInvite(
        userId: Int,
        organizationId: Int,
        invitedBy: Int,
        role: OrganizationRoleType
    ): OrganizationInvite {
        val organizationInvite = OrganizationInvite::class.java.getConstructor().newInstance()
        organizationInvite.userId = userId
        organizationInvite.organizationId = organizationId
        organizationInvite.invitedBy = invitedBy
        organizationInvite.role = roleRepository.getOne(role.id)
        organizationInvite.createdAt = ZonedDateTime.now()
        return organizationInviteRepository.save(organizationInvite)
    }

    private class TestUser {
        var email = "john@smith.com"
        var password = "Password157!"
        var firstName = "John"
        var lastName = "Smith"
        var phoneNumber = "0951234567"
        var authMethod = AuthMethod.EMAIL
    }

    private class TestContext {
        lateinit var user: User
        lateinit var organization: Organization
        lateinit var invitedByUser: User
    }
}
