package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.request.OrganizationRequest
import com.ampnet.crowdfundingbackend.controller.pojo.response.OrganizationListResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.OrganizationResponse
import com.ampnet.crowdfundingbackend.enums.OrganizationRoleType
import com.ampnet.crowdfundingbackend.enums.PrivilegeType
import com.ampnet.crowdfundingbackend.enums.UserRoleType
import com.ampnet.crowdfundingbackend.persistence.model.AuthMethod
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.persistence.repository.OrganizationDao
import com.ampnet.crowdfundingbackend.persistence.repository.RoleDao
import com.ampnet.crowdfundingbackend.persistence.repository.UserDao
import com.ampnet.crowdfundingbackend.security.WithMockCrowdfoundUser
import com.ampnet.crowdfundingbackend.service.OrganizationService
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.ZonedDateTime

class OrganizationControllerTest : ControllerTestBase() {

    private val organizationPath = "/organization"

    @Autowired
    private lateinit var organizationService: OrganizationService
    @Autowired
    private lateinit var userDao: UserDao
    @Autowired
    private lateinit var roleDao: RoleDao
    @Autowired
    private lateinit var organizationDao: OrganizationDao

    private val user: User by lazy {
        databaseCleanerService.deleteAllUsers()
        createUser(defaultEmail)
    }

    private lateinit var testContext: TestContext

    @Before
    fun initializeTestContext() {
        testContext = TestContext()
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToCreateOrganization() {
        suppose("User exists") {
            user.id
        }
        suppose("Organization does not exist") {
            databaseCleanerService.deleteAllOrganizations()
        }

        verify("User can create organization") {
            val name = "Organization name"
            val legalInfo = "Organization legal info"
            testContext.organizationRequest = OrganizationRequest(name, legalInfo)

            val result = mockMvc.perform(
                    post(organizationPath)
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                            .content(objectMapper.writeValueAsString(testContext.organizationRequest)))
                    .andExpect(status().isOk)
                    .andReturn()

            val organizationResponse: OrganizationResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(organizationResponse.name).isEqualTo(testContext.organizationRequest.name)
            assertThat(organizationResponse.legalInfo).isEqualTo(testContext.organizationRequest.legalInfo)
            assertThat(organizationResponse.createdByUser).isEqualTo(user.getFullName())
            assertThat(organizationResponse.id).isNotNull()
            assertThat(organizationResponse.approved).isFalse()
            assertThat(organizationResponse.documents).isEmpty()
            assertThat(organizationResponse.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())

            testContext.organizationId = organizationResponse.id
        }
        verify("Organization is stored in database") {
            val organization = organizationService.findOrganizationById(testContext.organizationId)
            assertThat(organization).isNotNull
            assertThat(organization!!.name).isEqualTo(testContext.organizationRequest.name)
            assertThat(organization.legalInfo).isEqualTo(testContext.organizationRequest.legalInfo)
            assertThat(organization.createdByUser.id).isEqualTo(user.id)
            assertThat(organization.id).isNotNull()
            assertThat(organization.approved).isFalse()
            assertThat(organization.documents).isEmpty()
            assertThat(organization.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
        }
        verify("Organization has only admin user") {
            val users = organizationService.findAllUsersFromOrganization(testContext.organizationId)
            assertThat(users).hasSize(1)
            val admin = users.first()
            assertThat(admin.id).isEqualTo(user.id)

            val memberships = admin.organizations
            assertThat(memberships).isNotNull
            assertThat(memberships!!).hasSize(1)
            val membership = memberships[0]
            assertThat(membership.userId).isEqualTo(user.id)
            assertThat(membership.organizationId).isEqualTo(testContext.organizationId)
            assertThat(membership.role.name).isEqualTo(OrganizationRoleType.ORG_ADMIN.name)
            assertThat(membership.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGetOrganization() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization")
        }

        verify("User can get organization with id") {
            val result = mockMvc.perform(get("$organizationPath/${testContext.organization.id}"))
                    .andExpect(status().isOk)
                    .andReturn()

            val organizationResponse: OrganizationResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(organizationResponse.name).isEqualTo(testContext.organization.name)
            assertThat(organizationResponse.legalInfo).isEqualTo(testContext.organization.legalInfo)
            assertThat(organizationResponse.id).isEqualTo(testContext.organization.id)
            assertThat(organizationResponse.approved).isEqualTo(testContext.organization.approved)
            assertThat(organizationResponse.documents).isEqualTo(testContext.organization.documents)
            assertThat(organizationResponse.createdAt).isEqualTo(testContext.organization.createdAt)
            assertThat(organizationResponse.createdByUser)
                    .isEqualTo(testContext.organization.createdByUser.getFullName())
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustReturnListOfOrganizations() {
        suppose("Multiple organizations exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization")
            createOrganization("test 2")
            createOrganization("test 3")
        }

        verify("User can get all organizations") {
            val result = mockMvc.perform(get(organizationPath))
                    .andExpect(status().isOk)
                    .andReturn()

            val organizationResponse: OrganizationListResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(organizationResponse.organizations).hasSize(3)
            assertThat(organizationResponse.organizations.map { it.name }).contains(testContext.organization.name)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PWA_ORG])
    fun mustBeAbleToApproveOrganization() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("Approve organization")
        }

        verify("Admin can approve organization") {
            val result = mockMvc.perform(
                    post("$organizationPath/${testContext.organization.id}/approve")
                            .contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andExpect(status().isOk)
                    .andReturn()

            val organizationResponse: OrganizationResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(organizationResponse.approved).isTrue()
        }
        verify("Organization is approved") {
            val organization = organizationService.findOrganizationById(testContext.organization.id)
            assertThat(organization).isNotNull
            assertThat(organization!!.approved).isTrue()
            assertThat(organization.updatedAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(organization.approvedBy).isNotNull
            assertThat(organization.approvedBy!!.id).isEqualTo(user.id)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustNotBeAbleApproveOrganizationWithoutPrivilege() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("Approve organization")
        }

        verify("User without privilege cannot approve organization") {
            mockMvc.perform(
                    post("$organizationPath/${testContext.organization.id}/approve")
                            .contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andExpect(status().isForbidden)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustReturnNotFoundForNonExistingOrganization() {
        verify("Response not found for non existing organization") {
            mockMvc.perform(get("$organizationPath/1299"))
                    .andExpect(status().isNotFound)
        }
    }

    private fun createUser(email: String): User {
        val user = User::class.java.newInstance()
        user.authMethod = AuthMethod.EMAIL
        user.createdAt = ZonedDateTime.now()
        user.email = email
        user.enabled = true
        user.firstName = "First"
        user.lastName = "Last"
        user.role = roleDao.getOne(UserRoleType.USER.id)
        return userDao.save(user)
    }

    private fun createOrganization(name: String): Organization {
        val organization = Organization::class.java.newInstance()
        organization.name = name
        organization.legalInfo = "some legal info"
        organization.createdAt = ZonedDateTime.now()
        organization.approved = true
        organization.createdByUser = user
        organization.documents = listOf("hash1", "hash2", "hash3")
        return organizationDao.save(organization)
    }

    private class TestContext {
        lateinit var organizationRequest: OrganizationRequest
        var organizationId: Int = -1
        lateinit var organization: Organization
    }
}
