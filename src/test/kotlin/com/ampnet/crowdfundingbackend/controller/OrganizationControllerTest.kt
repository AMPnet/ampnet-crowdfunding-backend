package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.request.OrganizationRequest
import com.ampnet.crowdfundingbackend.controller.pojo.response.DocumentResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.OrganizationListResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.OrganizationWithDocumentResponse
import com.ampnet.crowdfundingbackend.enums.OrganizationRoleType
import com.ampnet.crowdfundingbackend.enums.PrivilegeType
import com.ampnet.crowdfundingbackend.persistence.model.Document
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.security.WithMockCrowdfoundUser
import com.ampnet.crowdfundingbackend.service.OrganizationService
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
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

    private lateinit var testContext: TestContext

    @BeforeEach
    fun initializeTestContext() {
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
            assertThat(organizationWithDocumentResponse.id).isNotNull()
            assertThat(organizationWithDocumentResponse.approved).isFalse()
            assertThat(organizationWithDocumentResponse.documents).isEmpty()
            assertThat(organizationWithDocumentResponse.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(organizationWithDocumentResponse.walletHash).isNull()

            testContext.organizationId = organizationWithDocumentResponse.id
        }
        verify("Organization is stored in database") {
            val organization = organizationService.findOrganizationById(testContext.organizationId)
                    ?: fail("Organization must no be null")
            assertThat(organization.name).isEqualTo(testContext.organizationRequest.name)
            assertThat(organization.legalInfo).isEqualTo(testContext.organizationRequest.legalInfo)
            assertThat(organization.id).isNotNull()
            assertThat(organization.approved).isFalse()
            assertThat(organization.documents).isEmpty()
            assertThat(organization.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGetOrganization() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization", userUuid)
        }
        suppose("Organization has document") {
            createOrganizationDocument(testContext.organization, userUuid, "name", testContext.documentLink)
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
            assertThat(organizationWithDocumentResponse.walletHash).isEqualTo(testContext.walletHash)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_ORG])
    fun mustReturnListOfOrganizations() {
        suppose("Multiple organizations exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization", userUuid)
            createOrganization("test 2", userUuid)
            createOrganization("test 3", userUuid)
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
    @WithMockCrowdfoundUser
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
            testContext.organization = createOrganization("Approve organization", userUuid)
        }
        suppose("Organization has a wallet") {
            databaseCleanerService.deleteAllWallets()
            createWalletForOrganization(testContext.organization, testContext.walletHash)
        }
        suppose("Blockchain service will successfully approve organization") {
            Mockito.`when`(
                    blockchainService.activateOrganization(getWalletHash(testContext.organization.wallet))
            ).thenReturn("return")
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
                    ?: fail("Organization must no be null")
            assertThat(organization.approved).isTrue()
            assertThat(organization.updatedAt).isBeforeOrEqualTo(ZonedDateTime.now())
            val userApprovedBy = organization.approvedByUserUuid ?: fail("Approved UserUUID must not be null")
            assertThat(userApprovedBy).isEqualTo(userUuid)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustNotBeAbleApproveOrganizationWithoutPrivilege() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("Approve organization", userUuid)
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
    fun mustBeAbleToGetPersonalOrganizations() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization", userUuid)
        }
        suppose("User is a member of organization") {
            addUserToOrganization(userUuid, testContext.organization.id, OrganizationRoleType.ORG_MEMBER)
        }
        suppose("Another organization exists") {
            createOrganization("new organization", userUuid)
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
            testContext.organization = createOrganization("test organization", userUuid)
        }
        suppose("User is an admin of organization") {
            addUserToOrganization(userUuid, testContext.organization.id, OrganizationRoleType.ORG_ADMIN)
        }
        suppose("File storage will store document") {
            testContext.multipartFile = MockMultipartFile("file", "test.txt",
                    "text/plain", "Some document data".toByteArray())
            Mockito.`when`(
                    cloudStorageService.saveFile(testContext.multipartFile.originalFilename,
                            testContext.multipartFile.bytes)
            ).thenReturn(testContext.documentLink)
        }

        verify("User can add document to organization") {
            val result = mockMvc.perform(
                    fileUpload("$organizationPath/${testContext.organization.id}/document")
                            .file(testContext.multipartFile))
                    .andExpect(status().isOk)
                    .andReturn()

            val documentResponse: DocumentResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(documentResponse.id).isNotNull()
            assertThat(documentResponse.name).isEqualTo(testContext.multipartFile.originalFilename)
            assertThat(documentResponse.size).isEqualTo(testContext.multipartFile.size)
            assertThat(documentResponse.type).isEqualTo(testContext.multipartFile.contentType)
            assertThat(documentResponse.link).isEqualTo(testContext.documentLink)
        }
        verify("Document is stored in database and connected to organization") {
            val organizationDocuments = organizationService.findOrganizationById(testContext.organization.id)?.documents
                    ?: fail("Organization documents must not be null")
            assertThat(organizationDocuments).hasSize(1)

            val document = organizationDocuments[0]
            assertThat(document.name).isEqualTo(testContext.multipartFile.originalFilename)
            assertThat(document.size).isEqualTo(testContext.multipartFile.size)
            assertThat(document.type).isEqualTo(testContext.multipartFile.contentType)
            assertThat(document.link).isEqualTo(testContext.documentLink)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToDeleteOrganizationDocument() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization", userUuid)
        }
        suppose("Organization has 2 documents") {
            testContext.document =
                    createOrganizationDocument(testContext.organization, userUuid, "name", testContext.documentLink)
            createOrganizationDocument(testContext.organization, userUuid, "second.pdf", "second-link.pdf")
        }
        suppose("User is an admin of organization") {
            addUserToOrganization(userUuid, testContext.organization.id, OrganizationRoleType.ORG_ADMIN)
        }

        verify("User admin can delete document") {
            mockMvc.perform(
                    delete("$organizationPath/${testContext.organization.id}/document/${testContext.document.id}"))
                    .andExpect(status().isOk)
        }
        verify("Document is deleted") {
            val organizationWithDocument = organizationService.findOrganizationById(testContext.organization.id)
            assertThat(organizationWithDocument?.documents).hasSize(1).doesNotContain(testContext.document)
        }
    }

    private fun createOrganizationDocument(
        organization: Organization,
        createdByUserUuid: String,
        name: String,
        link: String,
        type: String = "document/type",
        size: Int = 100
    ): Document {
        val savedDocument = saveDocument(name, link, type, size, createdByUserUuid)
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
        val documentLink = "link"
        lateinit var document: Document
        lateinit var multipartFile: MockMultipartFile
        val walletHash = "0x4e4ee58ff3a9e9e78c2dfdbac0d1518e4e1039f9189267e1dc8d3e35cbdf7892"
    }
}
