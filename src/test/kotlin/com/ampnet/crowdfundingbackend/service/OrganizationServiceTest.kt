package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.enums.OrganizationRoleType
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.ResourceAlreadyExistsException
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.persistence.model.Document
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.service.impl.StorageServiceImpl
import com.ampnet.crowdfundingbackend.service.impl.OrganizationServiceImpl
import com.ampnet.crowdfundingbackend.service.pojo.DocumentSaveRequest
import com.ampnet.crowdfundingbackend.service.pojo.OrganizationServiceRequest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.time.ZonedDateTime
import java.util.UUID

class OrganizationServiceTest : JpaServiceTestBase() {

    private val organizationService: OrganizationService by lazy {
        val storageServiceImpl = StorageServiceImpl(documentRepository, cloudStorageService)
        OrganizationServiceImpl(organizationRepository, membershipRepository,
                roleRepository, mockedBlockchainService, storageServiceImpl)
    }
    private lateinit var organization: Organization

    private lateinit var testContext: TestContext

    @BeforeEach
    fun initTestContext() {
        databaseCleanerService.deleteAllOrganizations()
        organization = createOrganization("test org", userUuid)
        testContext = TestContext()
    }

    @Test
    fun mustBeAbleToAddUserAsAdminToOrganization() {
        suppose("User exists without any memberships") {
            databaseCleanerService.deleteAllOrganizationMemberships()
        }
        suppose("User is added as admin") {
            organizationService.addUserToOrganization(userUuid, organization.id, OrganizationRoleType.ORG_ADMIN)
        }

        verify("User has admin role") {
            verifyUserMembership(userUuid, organization.id, OrganizationRoleType.ORG_ADMIN)
        }
    }

    @Test
    fun mustBeAbleToAddUserAsMemberToOrganization() {
        suppose("User exists without any memberships") {
            databaseCleanerService.deleteAllOrganizationMemberships()
        }
        suppose("User is added to organization as member") {
            organizationService.addUserToOrganization(userUuid, organization.id, OrganizationRoleType.ORG_MEMBER)
        }

        verify("User has member role") {
            verifyUserMembership(userUuid, organization.id, OrganizationRoleType.ORG_MEMBER)
        }
    }

    @Test
    fun userCanHaveOnlyOneRoleInOrganization() {
        suppose("User exists without any memberships") {
            databaseCleanerService.deleteAllOrganizationMemberships()
        }
        suppose("User is added to organization as member") {
            organizationService.addUserToOrganization(userUuid, organization.id, OrganizationRoleType.ORG_MEMBER)
        }

        verify("Service will throw an exception for adding second role to the user in the same organization") {
            assertThrows<ResourceAlreadyExistsException> {
                organizationService.addUserToOrganization(userUuid, organization.id, OrganizationRoleType.ORG_ADMIN)
            }
        }
    }

    @Test
    fun mustThrowExceptionForApprovingNonExistingOrganization() {
        verify("Service will throw an exception if organization is missing") {
            val exception = assertThrows<ResourceNotFoundException> {
                organizationService.approveOrganization(0, true, userUuid)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.ORG_MISSING)
        }
    }

    @Test
    fun mustThrowExceptionForApprovingOrganizationWithoutWallet() {
        verify("Service will throw an excpetion if organization is missing a wallet") {
            val exception = assertThrows<ResourceNotFoundException> {
                organizationService.approveOrganization(organization.id, true, userUuid)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_MISSING)
        }
    }

    @Test
    fun userCanGetListOfPersonalOrganizations() {
        suppose("User is a member of two organizations") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            testContext.secondOrganization = createOrganization("Second org", userUuid)

            organizationService.addUserToOrganization(userUuid, organization.id, OrganizationRoleType.ORG_MEMBER)
            organizationService.addUserToOrganization(
                    userUuid, testContext.secondOrganization.id, OrganizationRoleType.ORG_MEMBER)
        }

        verify("User is a member of two organizations") {
            val organizations = organizationService.findAllOrganizationsForUser(userUuid)
            assertThat(organizations).hasSize(2)
            assertThat(organizations.map { it.id }).contains(organization.id, testContext.secondOrganization.id)
        }
    }

    @Test
    fun mustGetOrganizationWithDocument() {
        suppose("Organization has document") {
            testContext.document = createOrganizationDocument(organization, userUuid, "test doc", "link")
        }

        verify("Service returns organization with document") {
            val organizationWithDocument = organizationService.findOrganizationById(organization.id)
                    ?: fail("Organization must not be null")
            assertThat(organizationWithDocument.id).isEqualTo(organization.id)
            assertThat(organizationWithDocument.documents).hasSize(1)
            val document = organizationWithDocument.documents?.first() ?: fail("Organization must have one document")
            verifyDocument(document, testContext.document)
        }
    }

    @Test
    fun mustGetOrganizationWithMultipleDocuments() {
        suppose("Organization has 3 documents") {
            createOrganizationDocument(organization, userUuid, "Doc 1", "link1")
            createOrganizationDocument(organization, userUuid, "Doc 2", "link2")
            createOrganizationDocument(organization, userUuid, "Doc 3", "link3")
        }

        verify("Service returns organization with documents") {
            val organizationWithDocument = organizationService.findOrganizationById(organization.id)
                    ?: fail("Organization must not be null")
            assertThat(organizationWithDocument.id).isEqualTo(organization.id)
            assertThat(organizationWithDocument.documents).hasSize(3)
            assertThat(organizationWithDocument.documents?.map { it.link })
                    .containsAll(listOf("link1", "link2", "link3"))
        }
    }

    @Test
    fun mustNotBeAbleDocumentToNonExistingOrganization() {
        verify("Service will throw an exception that organization is missing") {
            val request = DocumentSaveRequest("Data".toByteArray(), "name", 10, "type/some", userUuid)
            val exception = assertThrows<ResourceNotFoundException> {
                organizationService.addDocument(0, request)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.ORG_MISSING)
        }
    }

    @Test
    fun mustAppendNewDocumentToCurrentListOfDocuments() {
        suppose("Organization has 2 documents") {
            createOrganizationDocument(organization, userUuid, "Doc 1", "link1")
            createOrganizationDocument(organization, userUuid, "Doc 2", "link2")
        }
        suppose("File storage service will successfully store document") {
            testContext.documentSaveRequest =
                    DocumentSaveRequest("Data".toByteArray(), "name", 10, "type/some", userUuid)
            Mockito.`when`(
                cloudStorageService.saveFile(testContext.documentSaveRequest.name, testContext.documentSaveRequest.data)
            ).thenReturn(testContext.documentLink)
        }

        verify("Service will append new document") {
            val document = organizationService.addDocument(organization.id, testContext.documentSaveRequest)
            assertThat(document.id).isNotNull()
            assertThat(document.name).isEqualTo(testContext.documentSaveRequest.name)
            assertThat(document.size).isEqualTo(testContext.documentSaveRequest.size)
            assertThat(document.type).isEqualTo(testContext.documentSaveRequest.type)

            assertThat(document.link).isEqualTo(testContext.documentLink)
            assertThat(document.createdByUserUuid).isEqualTo(userUuid)
            assertThat(document.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
        }
        verify("Organization has 3 documents") {
            val organizationWithDocuments = organizationService.findOrganizationById(organization.id)
                    ?: fail("Organization documents must not be null")
            assertThat(organizationWithDocuments.documents).hasSize(3)
            assertThat(organizationWithDocuments.documents?.map { it.link }).contains(testContext.documentLink)
        }
    }

    @Test
    fun mustNotBeAbleToRemoveOrganizationDocumentForNonExistingOrganization() {
        verify("Service will throw an exception for non existing organization") {
            val exception = assertThrows<ResourceNotFoundException> {
                organizationService.removeDocument(0, 0)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.ORG_MISSING)
        }
    }

    @Test
    fun mustNotBeAbleToCreateOrganizationWithSameName() {
        verify("Service will throw an exception for same name exception") {
            val exception = assertThrows<ResourceAlreadyExistsException> {
                val request = OrganizationServiceRequest(organization.name, "legal", userUuid)
                organizationService.createOrganization(request)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.ORG_DUPLICATE_NAME)
        }
    }

    @Test
    fun mustBeAbleToGetMembersOfOrganization() {
        suppose("There are users in organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            organizationService.addUserToOrganization(userUuid, organization.id, OrganizationRoleType.ORG_ADMIN)
            organizationService.addUserToOrganization(
                    testContext.member, organization.id, OrganizationRoleType.ORG_MEMBER)
        }
        suppose("There is another organization with members") {
            val additionalOrganization = createOrganization("Second organization", userUuid)
            organizationService.addUserToOrganization(
                    UUID.randomUUID(), additionalOrganization.id, OrganizationRoleType.ORG_MEMBER)
        }

        verify("Service will list all members of organization") {
            val memberships = organizationService.getOrganizationMemberships(organization.id)
            assertThat(memberships).hasSize(2)
            assertThat(memberships.map { it.userUuid }).containsAll(listOf(userUuid, testContext.member))
        }
    }

    @Test
    fun mustBeAbleToRemoveUserFromOrganization() {
        suppose("There are users in organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            organizationService.addUserToOrganization(userUuid, organization.id, OrganizationRoleType.ORG_MEMBER)
        }

        verify("User can be removed from organization") {
            organizationService.removeUserFromOrganization(userUuid, organization.id)
        }
        verify("User is no longer member of organization") {
            val memberships = membershipRepository.findByOrganizationId(organization.id)
            assertThat(memberships).hasSize(0)
        }
    }

    private fun verifyDocument(receivedDocument: Document, savedDocument: Document) {
        assertThat(receivedDocument.id).isEqualTo(savedDocument.id)
        assertThat(receivedDocument.link).isEqualTo(savedDocument.link)
        assertThat(receivedDocument.name).isEqualTo(savedDocument.name)
        assertThat(receivedDocument.size).isEqualTo(savedDocument.size)
        assertThat(receivedDocument.type).isEqualTo(savedDocument.type)
        assertThat(receivedDocument.createdAt).isEqualTo(savedDocument.createdAt)
        assertThat(receivedDocument.createdByUserUuid).isEqualTo(savedDocument.createdByUserUuid)
    }

    private fun verifyUserMembership(userUuid: UUID, organizationId: Int, role: OrganizationRoleType) {
        val memberships = membershipRepository.findByUserUuid(userUuid)
        assertThat(memberships).hasSize(1)
        val membership = memberships[0]
        assertThat(membership.userUuid).isEqualTo(userUuid)
        assertThat(membership.organizationId).isEqualTo(organizationId)
        assertThat(OrganizationRoleType.fromInt(membership.role.id)).isEqualTo(role)
        assertThat(membership.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
    }

    private fun createOrganizationDocument(
        organization: Organization,
        createdByUserUuid: UUID,
        name: String,
        link: String,
        type: String = "document/type",
        size: Int = 100
    ): Document {
        val document = saveDocument(name, link, createdByUserUuid, type, size)
        val documents = organization.documents.orEmpty().toMutableList()
        documents.add(document)
        organization.documents = documents
        organizationRepository.save(organization)
        return document
    }

    private class TestContext {
        lateinit var secondOrganization: Organization
        lateinit var document: Document
        lateinit var documentSaveRequest: DocumentSaveRequest
        val documentLink = "link"
        val member: UUID = UUID.randomUUID()
    }
}
