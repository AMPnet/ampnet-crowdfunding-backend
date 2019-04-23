package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.request.OrganizationInviteRequest
import com.ampnet.crowdfundingbackend.controller.pojo.request.OrganizationRequest
import com.ampnet.crowdfundingbackend.controller.pojo.response.DocumentResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.OrganizationListResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.OrganizationWithDocumentResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.OrganizationUserResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.OrganizationUsersListResponse
import com.ampnet.crowdfundingbackend.enums.OrganizationRoleType
import com.ampnet.crowdfundingbackend.enums.PrivilegeType
import com.ampnet.crowdfundingbackend.enums.UserRoleType
import com.ampnet.crowdfundingbackend.persistence.model.Document
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.OrganizationInvite
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.persistence.repository.OrganizationInviteRepository
import com.ampnet.crowdfundingbackend.security.WithMockCrowdfoundUser
import com.ampnet.crowdfundingbackend.service.OrganizationService
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.ZonedDateTime
import org.springframework.mock.web.MockMultipartFile
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.fileUpload

class OrganizationControllerTest : ControllerTestBase() {

    private val organizationPath = "/organization"

    @Autowired
    private lateinit var organizationService: OrganizationService
    @Autowired
    private lateinit var inviteRepository: OrganizationInviteRepository

    private val user: User by lazy {
        databaseCleanerService.deleteAllUsers()
        createUser(defaultEmail)
    }

    private lateinit var testContext: TestContext

    @BeforeEach
    fun initializeTestContext() {
        user.id
        testContext = TestContext()
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToCreateOrganization() {
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

            val organizationWithDocumentResponse: OrganizationWithDocumentResponse =
                    objectMapper.readValue(result.response.contentAsString)
            assertThat(organizationWithDocumentResponse.name).isEqualTo(testContext.organizationRequest.name)
            assertThat(organizationWithDocumentResponse.legalInfo).isEqualTo(testContext.organizationRequest.legalInfo)
            assertThat(organizationWithDocumentResponse.createdByUser).isEqualTo(user.getFullName())
            assertThat(organizationWithDocumentResponse.id).isNotNull()
            assertThat(organizationWithDocumentResponse.approved).isFalse()
            assertThat(organizationWithDocumentResponse.documents).isEmpty()
            assertThat(organizationWithDocumentResponse.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(organizationWithDocumentResponse.walletHash).isNull()

            testContext.organizationId = organizationWithDocumentResponse.id
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
            testContext.organization = createOrganization("test organization", user)
        }
        suppose("Organization has document") {
            createOrganizationDocument(testContext.organization, user, "name", testContext.documentLink)
        }
        suppose("Organization has a wallet") {
            databaseCleanerService.deleteAllWallets()
            createWalletForOrganization(testContext.organization, testContext.walletHash)
        }

        verify("User can get organization with id") {
            val result = mockMvc.perform(get("$organizationPath/${testContext.organization.id}"))
                    .andExpect(status().isOk)
                    .andReturn()

            val organizationWithDocumentResponse: OrganizationWithDocumentResponse =
                    objectMapper.readValue(result.response.contentAsString)
            assertThat(organizationWithDocumentResponse.name).isEqualTo(testContext.organization.name)
            assertThat(organizationWithDocumentResponse.legalInfo).isEqualTo(testContext.organization.legalInfo)
            assertThat(organizationWithDocumentResponse.id).isEqualTo(testContext.organization.id)
            assertThat(organizationWithDocumentResponse.approved).isEqualTo(testContext.organization.approved)
            assertThat(organizationWithDocumentResponse.documents.size)
                    .isEqualTo(testContext.organization.documents?.size)
            assertThat(organizationWithDocumentResponse.createdAt).isEqualTo(testContext.organization.createdAt)
            assertThat(organizationWithDocumentResponse.createdByUser)
                    .isEqualTo(testContext.organization.createdByUser.getFullName())
            assertThat(organizationWithDocumentResponse.walletHash).isEqualTo(testContext.walletHash)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_ORG])
    fun mustReturnListOfOrganizations() {
        suppose("Multiple organizations exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization", user)
            createOrganization("test 2", user)
            createOrganization("test 3", user)
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
    @WithMockCrowdfoundUser(role = UserRoleType.USER)
    fun mustNotGetListOfAllOrganizationsWithoutPrivilege() {
        verify("User cannot get a list of all organizations with privilege PRA_ORG") {
            mockMvc.perform(get(organizationPath))
                    .andExpect(status().isForbidden)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PWA_ORG_APPROVE])
    fun mustBeAbleToApproveOrganization() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("Approve organization", user)
        }
        suppose("Organization has a wallet") {
            databaseCleanerService.deleteAllWallets()
            createWalletForOrganization(testContext.organization, testContext.walletHash)
        }
        suppose("Blockchain service will successfully approve organization") {
            Mockito.`when`(blockchainService.activateOrganization(testContext.organization.wallet!!.hash))
                    .thenReturn("return")
        }

        verify("Admin can approve organization") {
            val result = mockMvc.perform(
                    post("$organizationPath/${testContext.organization.id}/approve")
                            .contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andExpect(status().isOk)
                    .andReturn()

            val organizationWithDocumentResponse: OrganizationWithDocumentResponse =
                    objectMapper.readValue(result.response.contentAsString)
            assertThat(organizationWithDocumentResponse.approved).isTrue()
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
            testContext.organization = createOrganization("Approve organization", user)
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

    @Test
    @WithMockCrowdfoundUser
    fun mustReturnUsersListForOrganization() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization", user)
        }
        suppose("Organization admin is member of another organization") {
            val organization = createOrganization("org 2", user)
            addUserToOrganization(user.id, organization.id, OrganizationRoleType.ORG_MEMBER)
        }
        suppose("Organization has 2 users") {
            testContext.user2 = createUser("user2@test.com")
            addUserToOrganization(user.id, testContext.organization.id, OrganizationRoleType.ORG_ADMIN)
            addUserToOrganization(testContext.user2.id, testContext.organization.id, OrganizationRoleType.ORG_MEMBER)
        }

        verify("User can fetch all users for organization") {
            val result = mockMvc.perform(
                    get("$organizationPath/${testContext.organization.id}/users"))
                    .andExpect(status().isOk)
                    .andReturn()

            val response: OrganizationUsersListResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(response.users).hasSize(2)
            assertThat(response.users).contains(
                    OrganizationUserResponse(user.getFullName(), user.email, OrganizationRoleType.ORG_ADMIN))
            assertThat(response.users).contains(
                    OrganizationUserResponse(testContext.user2.getFullName(), testContext.user2.email,
                            OrganizationRoleType.ORG_MEMBER))
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun userOutsideOrganizationMustNotBeAbleToFetchOrganizationUsers() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization", user)
        }

        verify("User is not able fetch organization users from other organization") {
            mockMvc.perform(
                    get("$organizationPath/${testContext.organization.id}/users"))
                    .andExpect(status().isForbidden)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToInviteUserToOrganizationWithOrgAdminRole() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization", user)
        }
        suppose("User has admin role in the organization") {
            addUserToOrganization(user.id, testContext.organization.id, OrganizationRoleType.ORG_ADMIN)
        }
        suppose("Other user has non organization invites") {
            testContext.user2 = createUser("user2@test.com")
            databaseCleanerService.deleteAllOrganizationInvites()
        }

        verify("Admin user can invite user to his organization") {
            val request = OrganizationInviteRequest(testContext.user2.email, OrganizationRoleType.ORG_MEMBER)
            mockMvc.perform(
                    post("$organizationPath/${testContext.organization.id}/invite")
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk)
        }
        verify("Organization invite is stored in database") {
            val invites = inviteRepository.findAll()
            assertThat(invites).hasSize(1)
            val invite = invites.first()
            assertThat(invite.userId).isEqualTo(testContext.user2.id)
            assertThat(invite.organizationId).isEqualTo(testContext.organization.id)
            assertThat(invite.invitedBy).isEqualTo(user.id)
            assertThat(invite.role.id).isEqualTo(OrganizationRoleType.ORG_MEMBER.id)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustNotBeAbleToInviteUserToOrganizationWithoutOrgAdminRole() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization", user)
        }
        suppose("User has admin role in the organization") {
            addUserToOrganization(user.id, testContext.organization.id, OrganizationRoleType.ORG_MEMBER)
        }

        verify("User cannot invite other user without ORG_ADMIN role") {
            val request = OrganizationInviteRequest("some@user.ocm", OrganizationRoleType.ORG_MEMBER)
            mockMvc.perform(
                    post("$organizationPath/${testContext.organization.id}/invite")
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustNotBeAbleToInviteUserToOrganizationIfNotMemberOfOrganization() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization", user)
        }

        verify("User can invite user to organization if he is not a member of organization") {
            val request = OrganizationInviteRequest("some@user.ocm", OrganizationRoleType.ORG_MEMBER)
            mockMvc.perform(
                    post("$organizationPath/${testContext.organization.id}/invite")
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToRevokeUserInvitation() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization", user)
        }
        suppose("User has admin role in the organization") {
            addUserToOrganization(user.id, testContext.organization.id, OrganizationRoleType.ORG_ADMIN)
        }
        suppose("Other user has organization invites") {
            testContext.user2 = createUser("user2@test.com")
            inviteUserToOrganization(testContext.user2.id, testContext.organization.id, user.id,
                OrganizationRoleType.ORG_MEMBER)
        }

        verify("User can revoke invitaiton") {
            mockMvc.perform(
                    post("$organizationPath/${testContext.organization.id}/invite/${testContext.user2.id}/revoke"))
                    .andExpect(status().isOk)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGetPersonalOrganizations() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization", user)
        }
        suppose("User is a member of organization") {
            addUserToOrganization(user.id, testContext.organization.id, OrganizationRoleType.ORG_MEMBER)
        }
        suppose("Another organization exists") {
            createOrganization("new organization", user)
        }

        verify("User will organization that he is a member") {
            val result = mockMvc.perform(get("$organizationPath/personal"))
                    .andExpect(status().isOk)
                    .andReturn()

            val organizationResponse: OrganizationListResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(organizationResponse.organizations).hasSize(1)
            assertThat(organizationResponse.organizations.map { it.name }).contains(testContext.organization.name)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToStoreDocumentForOrganization() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization", user)
        }
        suppose("User is an admin of organization") {
            addUserToOrganization(user.id, testContext.organization.id, OrganizationRoleType.ORG_ADMIN)
        }
        suppose("IPFS will store document") {
            testContext.multipartFile = MockMultipartFile("file", "test.txt",
                    "text/plain", "Some document data".toByteArray())
//            Mockito.`when`(ipfsService.storeData(testContext.multipartFile.bytes, testContext.multipartFile.name))
//                    .thenReturn(IpfsFile(testContext.documentLink, testContext.multipartFile.name, null))
        }

        verify("User can add document to organization") {
            val result = mockMvc.perform(
                    fileUpload("$organizationPath/${testContext.organization.id}/document")
                            .file(testContext.multipartFile))
                    .andExpect(status().isOk)
                    .andReturn()

            val documentResponse: DocumentResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(documentResponse.id).isNotNull()
            assertThat(documentResponse.name).isEqualTo(testContext.multipartFile.name)
            assertThat(documentResponse.size).isEqualTo(testContext.multipartFile.size)
            assertThat(documentResponse.type).isEqualTo(testContext.multipartFile.contentType)

            // TODO: fix
//            assertThat(documentResponse.link).isEqualTo(testContext.documentLink)
        }
        verify("Document is stored in database and connected to organization") {
            val organizationWithDocument = organizationService.findOrganizationById(testContext.organization.id)
            assertThat(organizationWithDocument?.documents).hasSize(1)

            val document = organizationWithDocument!!.documents!![0]
            assertThat(document.name).isEqualTo(testContext.multipartFile.name)
            assertThat(document.size).isEqualTo(testContext.multipartFile.size)
            assertThat(document.type).isEqualTo(testContext.multipartFile.contentType)

            // TODO: fix
//            assertThat(document.link).isEqualTo(testContext.documentLink)
        }
    }

    private fun inviteUserToOrganization(userId: Int, organizationId: Int, invitedBy: Int, role: OrganizationRoleType) {
        val invitation = OrganizationInvite::class.java.getConstructor().newInstance()
        invitation.userId = userId
        invitation.organizationId = organizationId
        invitation.invitedBy = invitedBy
        invitation.createdAt = ZonedDateTime.now()
        invitation.role = roleRepository.getOne(role.id)
        inviteRepository.save(invitation)
    }

    private fun createOrganizationDocument(
        organization: Organization,
        createdBy: User,
        name: String,
        link: String,
        type: String = "document/type",
        size: Int = 100
    ): Document {
        val savedDocument = saveDocument(name, link, type, size, createdBy)
        val documents = organization.documents.orEmpty().toMutableList()
        documents.add(savedDocument)
        organization.documents = documents
        organizationRepository.save(organization)
        return savedDocument
    }

    private class TestContext {
        lateinit var organizationRequest: OrganizationRequest
        var organizationId: Int = -1
        lateinit var organization: Organization
        lateinit var user2: User
        val documentLink = "link"
        lateinit var multipartFile: MockMultipartFile
        val walletHash = "0x4e4ee58ff3a9e9e78c2dfdbac0d1518e4e1039f9189267e1dc8d3e35cbdf7892"
    }
}
