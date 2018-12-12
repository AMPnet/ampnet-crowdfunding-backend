package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.enums.OrganizationRoleType
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.ResourceAlreadyExistsException
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.OrganizationFollower
import com.ampnet.crowdfundingbackend.persistence.model.OrganizationInvite
import com.ampnet.crowdfundingbackend.persistence.model.OrganizationMembership
import com.ampnet.crowdfundingbackend.persistence.model.Role
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.persistence.repository.OrganizationRepository
import com.ampnet.crowdfundingbackend.persistence.repository.OrganizationFollowerRepository
import com.ampnet.crowdfundingbackend.persistence.repository.OrganizationInviteRepository
import com.ampnet.crowdfundingbackend.persistence.repository.OrganizationMembershipRepository
import com.ampnet.crowdfundingbackend.persistence.repository.RoleRepository
import com.ampnet.crowdfundingbackend.persistence.repository.UserRepository
import com.ampnet.crowdfundingbackend.service.MailService
import com.ampnet.crowdfundingbackend.service.OrganizationService
import com.ampnet.crowdfundingbackend.service.pojo.OrganizationInviteServiceRequest
import com.ampnet.crowdfundingbackend.service.pojo.OrganizationServiceRequest
import mu.KLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Service
class OrganizationServiceImpl(
    private val organizationRepository: OrganizationRepository,
    private val membershipRepository: OrganizationMembershipRepository,
    private val followerRepository: OrganizationFollowerRepository,
    private val inviteRepository: OrganizationInviteRepository,
    private val roleRepository: RoleRepository,
    private val userRepository: UserRepository,
    private val mailService: MailService
) : OrganizationService {

    companion object : KLogging()

    private val adminRole: Role by lazy { roleRepository.getOne(OrganizationRoleType.ORG_ADMIN.id) }
    private val memberRole: Role by lazy { roleRepository.getOne(OrganizationRoleType.ORG_MEMBER.id) }

    @Transactional
    override fun createOrganization(serviceRequest: OrganizationServiceRequest): Organization {
        val organization = Organization::class.java.getConstructor().newInstance()
        organization.name = serviceRequest.name
        organization.createdByUser = serviceRequest.owner
        organization.legalInfo = serviceRequest.legalInfo
        organization.documents = serviceRequest.documentHashes
        organization.approved = false
        organization.createdAt = ZonedDateTime.now()

        val savedOrganization = organizationRepository.save(organization)
        addUserToOrganization(serviceRequest.owner.id, organization.id, OrganizationRoleType.ORG_ADMIN)

        return savedOrganization
    }

    @Transactional(readOnly = true)
    override fun getAllOrganizations(): List<Organization> {
        return organizationRepository.findAll()
    }

    @Transactional(readOnly = true)
    override fun findOrganizationById(id: Int): Organization? {
        return ServiceUtils.wrapOptional(organizationRepository.findById(id))
    }

    @Transactional
    override fun approveOrganization(organizationId: Int, approve: Boolean, approvedBy: User): Organization {
        findOrganizationById(organizationId)?.let {
            it.approved = approve
            it.updatedAt = ZonedDateTime.now()
            it.approvedBy = approvedBy
            return it
        }
        throw ResourceNotFoundException(ErrorCode.ORG_MISSING, "Missing organization with id: $organizationId")
    }

    @Transactional(readOnly = true)
    override fun findAllUsersFromOrganization(organizationId: Int): List<User> {
        return userRepository.findAllUserForOrganization(organizationId)
    }

    @Transactional(readOnly = true)
    override fun findAllOrganizationsForUser(userId: Int): List<Organization> {
        return organizationRepository.findAllOrganizationsForUser(userId)
    }

    @Transactional(readOnly = true)
    override fun getOrganizationMemberships(organizationId: Int): List<OrganizationMembership> {
        return membershipRepository.findByOrganizationId(organizationId)
    }

    @Transactional
    override fun addUserToOrganization(
        userId: Int,
        organizationId: Int,
        role: OrganizationRoleType
    ): OrganizationMembership {
        // user can have only one membership(role) per one organization
        membershipRepository.findByOrganizationIdAndUserId(organizationId, userId).ifPresent {
            throw ResourceAlreadyExistsException(ErrorCode.ORG_DUPLICATE_USER,
                    "User ${it.userId} is already a member of this organization ${it.organizationId}")
        }

        val membership = OrganizationMembership::class.java.getConstructor().newInstance()
        membership.organizationId = organizationId
        membership.userId = userId
        membership.role = getRole(role)
        membership.createdAt = ZonedDateTime.now()
        return membershipRepository.save(membership)
    }

    @Transactional
    override fun inviteUserToOrganization(request: OrganizationInviteServiceRequest): OrganizationInvite {
        val user = userRepository.findByEmail(request.email).orElseThrow {
            ResourceNotFoundException(ErrorCode.USER_MISSING,
                    "User with email: ${request.email} does not exists")
        }

        inviteRepository.findByOrganizationIdAndUserId(request.organizationId, user.id).ifPresent {
            throw ResourceAlreadyExistsException(ErrorCode.ORG_DUPLICATE_INVITE,
                    "User is already invited to join organization")
        }

        val organizationInvite = OrganizationInvite::class.java.getConstructor().newInstance()
        organizationInvite.organizationId = request.organizationId
        organizationInvite.userId = user.id
        organizationInvite.role = getRole(request.roleType)
        organizationInvite.invitedBy = request.invitedByUser.id
        organizationInvite.createdAt = ZonedDateTime.now()

        val savedInvite = inviteRepository.save(organizationInvite)
        sendMailInvitationToJoinOrganization(request.email, request.invitedByUser, request.organizationId)
        return savedInvite
    }

    @Transactional
    override fun revokeInvitationToJoinOrganization(organizationId: Int, userId: Int) {
        inviteRepository.findByOrganizationIdAndUserId(organizationId, userId).ifPresent {
            inviteRepository.delete(it)
        }
    }

    @Transactional(readOnly = true)
    override fun getAllOrganizationInvitesForUser(userId: Int): List<OrganizationInvite> {
        return inviteRepository.findByUserIdWithUserAndOrganizationData(userId)
    }

    @Transactional
    override fun answerToOrganizationInvitation(userId: Int, join: Boolean, organizationId: Int) {
        inviteRepository.findByOrganizationIdAndUserId(organizationId, userId).ifPresent {
            if (join) {
                addUserToOrganization(it.userId, it.organizationId, OrganizationRoleType.fromInt(it.role.id)!!)
            }
            inviteRepository.delete(it)
        }
    }

    @Transactional
    override fun followOrganization(userId: Int, organizationId: Int): OrganizationFollower {
        ServiceUtils.wrapOptional(followerRepository.findByUserIdAndOrganizationId(userId, organizationId))?.let {
            return it
        }
        val follower = OrganizationFollower::class.java.getConstructor().newInstance()
        follower.userId = userId
        follower.organizationId = organizationId
        follower.createdAt = ZonedDateTime.now()
        return followerRepository.save(follower)
    }

    @Transactional
    override fun unfollowOrganization(userId: Int, organizationId: Int) {
        ServiceUtils.wrapOptional(followerRepository.findByUserIdAndOrganizationId(userId, organizationId))?.let {
            followerRepository.delete(it)
        }
    }

    private fun sendMailInvitationToJoinOrganization(to: String, invitedBy: User, invitedTo: Int) {
        organizationRepository.findById(invitedTo).ifPresent {
            logger.debug { "Sending invitation mail to user: $to for organization: ${it.name}" }
            mailService.sendOrganizationInvitationMail(to, invitedBy.getFullName(), it.name)
            return@ifPresent
        }
        logger.warn { "Trying to send invitation for non-existing organization: $invitedTo by user: $invitedBy" }
    }

    private fun getRole(role: OrganizationRoleType): Role {
        return when (role) {
            OrganizationRoleType.ORG_ADMIN -> adminRole
            OrganizationRoleType.ORG_MEMBER -> memberRole
        }
    }
}
