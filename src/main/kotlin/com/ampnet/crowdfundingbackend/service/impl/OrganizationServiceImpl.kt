package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.blockchain.BlockchainService
import com.ampnet.crowdfundingbackend.enums.OrganizationRoleType
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.ResourceAlreadyExistsException
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.persistence.model.Document
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.OrganizationMembership
import com.ampnet.crowdfundingbackend.persistence.model.Role
import com.ampnet.crowdfundingbackend.persistence.repository.OrganizationRepository
import com.ampnet.crowdfundingbackend.persistence.repository.OrganizationMembershipRepository
import com.ampnet.crowdfundingbackend.persistence.repository.RoleRepository
import com.ampnet.crowdfundingbackend.service.StorageService
import com.ampnet.crowdfundingbackend.service.OrganizationService
import com.ampnet.crowdfundingbackend.service.pojo.DocumentSaveRequest
import com.ampnet.crowdfundingbackend.service.pojo.OrganizationServiceRequest
import mu.KLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

@Service
class OrganizationServiceImpl(
    private val organizationRepository: OrganizationRepository,
    private val membershipRepository: OrganizationMembershipRepository,
    private val roleRepository: RoleRepository,
    private val blockchainService: BlockchainService,
    private val storageService: StorageService
) : OrganizationService {

    companion object : KLogging()

    private val adminRole: Role by lazy { roleRepository.getOne(OrganizationRoleType.ORG_ADMIN.id) }
    private val memberRole: Role by lazy { roleRepository.getOne(OrganizationRoleType.ORG_MEMBER.id) }

    @Transactional
    override fun createOrganization(serviceRequest: OrganizationServiceRequest): Organization {
        if (organizationRepository.findByName(serviceRequest.name).isPresent) {
            throw ResourceAlreadyExistsException(ErrorCode.ORG_DUPLICATE_NAME,
                    "Organization with name: ${serviceRequest.name} already exists")
        }

        val organization = Organization::class.java.getConstructor().newInstance()
        organization.name = serviceRequest.name
        organization.createdByUserUuid = serviceRequest.ownerUuid
        organization.legalInfo = serviceRequest.legalInfo
        organization.approved = false
        organization.createdAt = ZonedDateTime.now()

        val savedOrganization = organizationRepository.save(organization)
        addUserToOrganization(serviceRequest.ownerUuid, organization.id, OrganizationRoleType.ORG_ADMIN)

        return savedOrganization
    }

    @Transactional(readOnly = true)
    override fun getAllOrganizations(): List<Organization> {
        return organizationRepository.findAll()
    }

    @Transactional(readOnly = true)
    override fun findOrganizationById(id: Int): Organization? {
        return ServiceUtils.wrapOptional(organizationRepository.findByIdWithDocuments(id))
    }

    @Transactional(readOnly = true)
    override fun findOrganizationByIdWithWallet(id: Int): Organization? {
        val organization = ServiceUtils.wrapOptional(organizationRepository.findById(id))
        organization?.wallet
        return organization
    }

    @Transactional
    override fun approveOrganization(organizationId: Int, approve: Boolean, approvedBy: UUID): Organization {
        val organization = getOrganization(organizationId)
        val wallet = organization.wallet ?: throw ResourceNotFoundException(
                ErrorCode.WALLET_MISSING, "Organization need to have wallet before it can be approved"
        )
        organization.approved = approve
        organization.updatedAt = ZonedDateTime.now()
        organization.approvedByUserUuid = approvedBy
        blockchainService.activateOrganization(wallet.hash)
        return organization
    }

    @Transactional(readOnly = true)
    override fun findAllOrganizationsForUser(userUuid: UUID): List<Organization> {
        return organizationRepository.findAllOrganizationsForUserUuid(userUuid)
    }

    @Transactional(readOnly = true)
    override fun getOrganizationMemberships(organizationId: Int): List<OrganizationMembership> {
        return membershipRepository.findByOrganizationId(organizationId)
    }

    @Transactional
    override fun addUserToOrganization(
        userUuid: UUID,
        organizationId: Int,
        role: OrganizationRoleType
    ): OrganizationMembership {
        // user can have only one membership(role) per one organization
        membershipRepository.findByOrganizationIdAndUserUuid(organizationId, userUuid).ifPresent {
            throw ResourceAlreadyExistsException(ErrorCode.ORG_DUPLICATE_USER,
                    "User ${it.userUuid} is already a member of this organization ${it.organizationId}")
        }

        val membership = OrganizationMembership::class.java.getConstructor().newInstance()
        membership.organizationId = organizationId
        membership.userUuid = userUuid
        membership.role = getRole(role)
        membership.createdAt = ZonedDateTime.now()
        return membershipRepository.save(membership)
    }

    @Transactional
    override fun removeUserFromOrganization(userUuid: UUID, organizationId: Int) {
        membershipRepository.findByOrganizationIdAndUserUuid(organizationId, userUuid).ifPresent {
            membershipRepository.delete(it)
        }
    }

    @Transactional
    override fun addDocument(organizationId: Int, request: DocumentSaveRequest): Document {
        val organization = getOrganization(organizationId)
        val document = storageService.saveDocument(request)
        addDocumentToOrganization(organization, document)
        return document
    }

    @Transactional
    override fun removeDocument(organizationId: Int, documentId: Int) {
        val organization = getOrganization(organizationId)
        val storedDocuments = organization.documents.orEmpty().toMutableList()
        storedDocuments.firstOrNull { it.id == documentId }.let {
            storedDocuments.remove(it)
            organization.documents = storedDocuments
            organizationRepository.save(organization)
        }
    }

    private fun getOrganization(organizationId: Int): Organization =
            findOrganizationById(organizationId)
                    ?: throw ResourceNotFoundException(ErrorCode.ORG_MISSING,
                            "Missing organization with id: $organizationId")

    private fun addDocumentToOrganization(organization: Organization, document: Document) {
        val documents = organization.documents.orEmpty().toMutableList()
        documents += document
        organization.documents = documents
        organizationRepository.save(organization)
    }

    private fun getRole(role: OrganizationRoleType): Role {
        return when (role) {
            OrganizationRoleType.ORG_ADMIN -> adminRole
            OrganizationRoleType.ORG_MEMBER -> memberRole
        }
    }
}
