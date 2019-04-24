package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.enums.OrganizationRoleType
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.ResourceAlreadyExistsException
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.persistence.model.Document
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.service.impl.DocumentServiceImpl
import com.ampnet.crowdfundingbackend.service.impl.FileStorageServiceImpl
import com.ampnet.crowdfundingbackend.service.impl.MailServiceImpl
import com.ampnet.crowdfundingbackend.service.impl.OrganizationServiceImpl
import com.ampnet.crowdfundingbackend.service.pojo.DocumentSaveRequest
import com.ampnet.crowdfundingbackend.service.pojo.OrganizationInviteServiceRequest
import com.ampnet.crowdfundingbackend.service.pojo.OrganizationServiceRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.time.ZonedDateTime

class OrganizationServiceTest : JpaServiceTestBase() {

    private val mailService: MailServiceImpl = Mockito.mock(MailServiceImpl::class.java)
    private val fileStorageService: FileStorageServiceImpl = Mockito.mock(FileStorageServiceImpl::class.java)

    private val organizationService: OrganizationService by lazy {
        val documentServiceImpl = DocumentServiceImpl(documentRepository, fileStorageService)
        OrganizationServiceImpl(organizationRepository, membershipRepository, followerRepository, inviteRepository,
                roleRepository, userRepository, mailService, mockedBlockchainService, documentServiceImpl)
    }

    private val user: User by lazy {
        databaseCleanerService.deleteAllUsers()
        createUser("test@email.com", "First", "Last")
    }
    private val organization: Organization by lazy {
        databaseCleanerService.deleteAllOrganizations()
        createOrganization("test org", user)
    }

    private lateinit var testContext: TestContext

    @BeforeEach
    fun initTestContext() {
        user.id
        organization.id
        testContext = TestContext()
    }

    @Test
    fun mustBeAbleToAddUserAsAdminToOrganization() {
        suppose("User exists without any memberships") {
            databaseCleanerService.deleteAllOrganizationMemberships()
        }
        suppose("User is added as admin") {
            organizationService.addUserToOrganization(user.id, organization.id, OrganizationRoleType.ORG_ADMIN)
        }

        verify("User has admin role") {
            verifyUserMembership(user.id, organization.id, OrganizationRoleType.ORG_ADMIN)
        }
    }

    @Test
    fun mustBeAbleToAddUserAsMemberToOrganization() {
        suppose("User exists without any memberships") {
            databaseCleanerService.deleteAllOrganizationMemberships()
        }
        suppose("User is added to organization as member") {
            organizationService.addUserToOrganization(user.id, organization.id, OrganizationRoleType.ORG_MEMBER)
        }

        verify("User has member role") {
            verifyUserMembership(user.id, organization.id, OrganizationRoleType.ORG_MEMBER)
        }
    }

    @Test
    fun userCanHaveOnlyOneRoleInOrganization() {
        suppose("User exists without any memberships") {
            databaseCleanerService.deleteAllOrganizationMemberships()
        }
        suppose("User is added to organization as member") {
            organizationService.addUserToOrganization(user.id, organization.id, OrganizationRoleType.ORG_MEMBER)
        }

        verify("Service will throw an exception for adding second role to the user in the same organization") {
            assertThrows<ResourceAlreadyExistsException> {
                organizationService.addUserToOrganization(user.id, organization.id, OrganizationRoleType.ORG_ADMIN)
            }
        }
    }

    @Test
    fun userCanFollowOrganization() {
        suppose("User exists without following organizations") {
            databaseCleanerService.deleteAllOrganizationFollowers()
        }
        suppose("User started to follow the organization") {
            organizationService.followOrganization(user.id, organization.id)
        }

        verify("User is following the organization") {
            val followers = followerRepository.findByOrganizationId(organization.id)
            assertThat(followers).hasSize(1)

            val follower = followers[0]
            assertThat(follower.userId).isEqualTo(user.id)
            assertThat(follower.organizationId).isEqualTo(organization.id)
            assertThat(follower.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
        }
    }

    @Test
    fun userCanUnFollowOrganization() {
        suppose("User is following the organization") {
            databaseCleanerService.deleteAllOrganizationFollowers()
            organizationService.followOrganization(user.id, organization.id)
            val followers = followerRepository.findByOrganizationId(organization.id)
            assertThat(followers).hasSize(1)
        }
        suppose("User un followed the organization") {
            organizationService.unfollowOrganization(user.id, organization.id)
        }

        verify("User is not following the organization") {
            val followers = followerRepository.findByOrganizationId(organization.id)
            assertThat(followers).hasSize(0)
        }
    }

    @Test
    fun adminUserCanInviteOtherUserToOrganization() {
        suppose("User is admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            organizationService.addUserToOrganization(user.id, organization.id, OrganizationRoleType.ORG_ADMIN)
        }

        verify("The admin can invite user to organization") {
            testContext.invitedUser = createUser("invited@user.com", "Invited", "User")
            val request = OrganizationInviteServiceRequest(
                    testContext.invitedUser.email, OrganizationRoleType.ORG_MEMBER, organization.id, user)
            organizationService.inviteUserToOrganization(request)
        }
        verify("Invitation is stored in database") {
            val optionalInvitation =
                    inviteRepository.findByOrganizationIdAndUserId(organization.id, testContext.invitedUser.id)
            assertThat(optionalInvitation).isPresent
            val invitation = optionalInvitation.get()
            assertThat(invitation.userId).isEqualTo(testContext.invitedUser.id)
            assertThat(invitation.organizationId).isEqualTo(organization.id)
            assertThat(invitation.invitedBy).isEqualTo(user.id)
            assertThat(OrganizationRoleType.fromInt(invitation.role.id)).isEqualTo(OrganizationRoleType.ORG_MEMBER)
            assertThat(invitation.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
        }
        verify("Sending mail invitation is called") {
            Mockito.verify(mailService, Mockito.times(1))
                    .sendOrganizationInvitationMail(
                        testContext.invitedUser.email, user.getFullName(), organization.name)
        }
    }

    @Test
    fun mustThrowErrorForDuplicateOrganizationInvite() {
        suppose("User has organization invite") {
            databaseCleanerService.deleteAllOrganizationInvites()
            testContext.invitedUser = createUser("invited@user.com", "Invited", "User")
            val request = OrganizationInviteServiceRequest(
                    testContext.invitedUser.email, OrganizationRoleType.ORG_MEMBER, organization.id, user)
            organizationService.inviteUserToOrganization(request)
        }

        verify("Service will throw an error for duplicate user invite to organization") {
            val request = OrganizationInviteServiceRequest(
                    testContext.invitedUser.email, OrganizationRoleType.ORG_MEMBER, organization.id, user)
            assertThrows<ResourceAlreadyExistsException> {
                organizationService.inviteUserToOrganization(request)
            }
        }
    }

    @Test
    fun mustThrowExceptionForApprovingNonExistingOrganization() {
        verify("Service will throw an exception if organization is missing") {
            val exception = assertThrows<ResourceNotFoundException> {
                organizationService.approveOrganization(0, true, user)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.ORG_MISSING)
        }
    }

    @Test
    fun mustThrowExceptionForApprovingOrganizationWithoutWallet() {
        verify("Service will throw an excpetion if organization is missing a wallet") {
            val exception = assertThrows<ResourceNotFoundException> {
                organizationService.approveOrganization(organization.id, true, user)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_MISSING)
        }
    }

    @Test
    fun userCanGetListOfHisOrganizations() {
        suppose("User is a member of two organizations") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            testContext.secondOrganization = createOrganization("Second org", user)

            organizationService.addUserToOrganization(user.id, organization.id, OrganizationRoleType.ORG_MEMBER)
            organizationService.addUserToOrganization(
                user.id, testContext.secondOrganization.id, OrganizationRoleType.ORG_MEMBER)
        }

        verify("User is a member of two organizations") {
            val organizations = organizationService.findAllOrganizationsForUser(user.id)
            assertThat(organizations).hasSize(2)
            assertThat(organizations.map { it.id }).contains(organization.id, testContext.secondOrganization.id)
        }
    }

    @Test
    fun mustGetOrganizationWithDocument() {
        suppose("Organization has document") {
            testContext.document = createOrganizationDocument(organization, user, "test doc", "link")
        }

        verify("Service returns organization with document") {
            val organizationWithDocument = organizationService.findOrganizationById(organization.id)
            assertThat(organizationWithDocument).isNotNull
            assertThat(organizationWithDocument!!.id).isEqualTo(organization.id)
            assertThat(organizationWithDocument.documents).hasSize(1)
            verifyDocument(organizationWithDocument.documents!!.first(), testContext.document)
        }
    }

    @Test
    fun mustGetOrganizationWithMultipleDocuments() {
        suppose("Organization has 3 documents") {
            createOrganizationDocument(organization, user, "Doc 1", "link1")
            createOrganizationDocument(organization, user, "Doc 2", "link2")
            createOrganizationDocument(organization, user, "Doc 3", "link3")
        }

        verify("Service returns organization with documents") {
            val organizationWithDocument = organizationService.findOrganizationById(organization.id)
            assertThat(organizationWithDocument).isNotNull
            assertThat(organizationWithDocument!!.id).isEqualTo(organization.id)
            assertThat(organizationWithDocument.documents).hasSize(3)
            assertThat(organizationWithDocument.documents!!.map { it.link })
                    .containsAll(listOf("link1", "link2", "link3"))
        }
    }

    @Test
    fun mustNotBeAbleDocumentToNonExistingOrganization() {
        verify("Service will throw an exception that organization is missing") {
            val request = DocumentSaveRequest("Data".toByteArray(), "name", 10, "type/some", user)
            val exception = assertThrows<ResourceNotFoundException> {
                organizationService.addDocument(0, request)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.ORG_MISSING)
        }
    }

    @Test
    fun mustAppendNewDocumentToCurrentListOfDocuments() {
        suppose("Organization has 2 documents") {
            createOrganizationDocument(organization, user, "Doc 1", "link1")
            createOrganizationDocument(organization, user, "Doc 2", "link2")
        }
        suppose("File storage service will successfully store document") {
            testContext.documentSaveRequest =
                    DocumentSaveRequest("Data".toByteArray(), "name", 10, "type/some", user)
            Mockito.`when`(
                fileStorageService.saveFile(testContext.documentSaveRequest.name, testContext.documentSaveRequest.data)
            ).thenReturn(testContext.documentLink)
        }

        verify("Service will append new document") {
            val document = organizationService.addDocument(organization.id, testContext.documentSaveRequest)
            assertThat(document.id).isNotNull()
            assertThat(document.name).isEqualTo(testContext.documentSaveRequest.name)
            assertThat(document.size).isEqualTo(testContext.documentSaveRequest.size)
            assertThat(document.type).isEqualTo(testContext.documentSaveRequest.type)

            assertThat(document.link).isEqualTo(testContext.documentLink)
            assertThat(document.createdBy.id).isEqualTo(user.id)
            assertThat(document.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
        }
        verify("Organization has 3 documents") {
            val organizationWithDocuments = organizationService.findOrganizationById(organization.id)
            assertThat(organizationWithDocuments).isNotNull
            assertThat(organizationWithDocuments!!.documents).hasSize(3)

            assertThat(organizationWithDocuments.documents!!.map { it.link }).contains(testContext.documentLink)
        }
    }

    @Test
    fun mustNotBeAbleToCreateOrganizationWithSameName() {
        verify("Service will throw an exception for same name exception") {
            val exception = assertThrows<ResourceAlreadyExistsException> {
                val request = OrganizationServiceRequest(organization.name, "legal", user)
                organizationService.createOrganization(request)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.ORG_DUPLICATE_NAME)
        }
    }

    private fun verifyDocument(receivedDocument: Document, savedDocument: Document) {
        assertThat(receivedDocument.id).isEqualTo(savedDocument.id)
        assertThat(receivedDocument.link).isEqualTo(savedDocument.link)
        assertThat(receivedDocument.name).isEqualTo(savedDocument.name)
        assertThat(receivedDocument.size).isEqualTo(savedDocument.size)
        assertThat(receivedDocument.type).isEqualTo(savedDocument.type)
        assertThat(receivedDocument.createdAt).isEqualTo(savedDocument.createdAt)
        assertThat(receivedDocument.createdBy.id).isEqualTo(savedDocument.createdBy.id)
    }

    private fun verifyUserMembership(userId: Int, organizationId: Int, role: OrganizationRoleType) {
        val memberships = membershipRepository.findByUserId(userId)
        assertThat(memberships).hasSize(1)
        val membership = memberships[0]
        assertThat(membership.userId).isEqualTo(userId)
        assertThat(membership.organizationId).isEqualTo(organizationId)
        assertThat(OrganizationRoleType.fromInt(membership.role.id)).isEqualTo(role)
        assertThat(membership.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
    }

    private fun createOrganizationDocument(
        organization: Organization,
        createdBy: User,
        name: String,
        link: String,
        type: String = "document/type",
        size: Int = 100
    ): Document {
        val document = Document::class.java.newInstance()
        document.name = name
        document.link = link
        document.type = type
        document.size = size
        document.createdBy = createdBy
        document.createdAt = ZonedDateTime.now()
        val savedDocument = documentRepository.save(document)

        val documents = organization.documents.orEmpty().toMutableList()
        documents.add(savedDocument)

        organization.documents = documents
        organizationRepository.save(organization)
        return savedDocument
    }

    private class TestContext {
        lateinit var invitedUser: User
        lateinit var secondOrganization: Organization
        lateinit var document: Document
        lateinit var documentSaveRequest: DocumentSaveRequest
        val documentLink = "link"
    }
}
