package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.enums.OrganizationRoleType
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.ResourceAlreadyExistsException
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.OrganizationFollower
import com.ampnet.crowdfundingbackend.persistence.model.OrganizationInvitation
import com.ampnet.crowdfundingbackend.persistence.model.Role
import com.ampnet.crowdfundingbackend.persistence.repository.OrganizationFollowerRepository
import com.ampnet.crowdfundingbackend.persistence.repository.OrganizationInviteRepository
import com.ampnet.crowdfundingbackend.persistence.repository.RoleRepository
import com.ampnet.crowdfundingbackend.service.MailService
import com.ampnet.crowdfundingbackend.service.OrganizationInviteService
import com.ampnet.crowdfundingbackend.service.OrganizationService
import com.ampnet.crowdfundingbackend.service.pojo.OrganizationInviteAnswerRequest
import com.ampnet.crowdfundingbackend.service.pojo.OrganizationInviteServiceRequest
import mu.KLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

@Service
class OrganizationInviteServiceImpl(
    private val inviteRepository: OrganizationInviteRepository,
    private val followerRepository: OrganizationFollowerRepository,
    private val roleRepository: RoleRepository,
    private val mailService: MailService,
    private val organizationService: OrganizationService
) : OrganizationInviteService {

    companion object : KLogging()

    private val adminRole: Role by lazy { roleRepository.getOne(OrganizationRoleType.ORG_ADMIN.id) }
    private val memberRole: Role by lazy { roleRepository.getOne(OrganizationRoleType.ORG_MEMBER.id) }

    @Transactional
    override fun sendInvitation(request: OrganizationInviteServiceRequest): OrganizationInvitation {
        val invitedToOrganization = organizationService.findOrganizationById(request.organizationId)
                ?: throw ResourceNotFoundException(ErrorCode.ORG_MISSING,
                        "Missing organization with id: ${request.organizationId}")

        inviteRepository.findByOrganizationIdAndEmail(request.organizationId, request.email).ifPresent {
            throw ResourceAlreadyExistsException(ErrorCode.ORG_DUPLICATE_INVITE,
                    "User is already invited to join organization")
        }

        val organizationInvite = OrganizationInvitation::class.java.getConstructor().newInstance()
        organizationInvite.organizationId = request.organizationId
        organizationInvite.email = request.email
        organizationInvite.role = getRole(request.roleType)
        organizationInvite.invitedByUserUuid = request.invitedByUserUuid
        organizationInvite.createdAt = ZonedDateTime.now()

        val savedInvite = inviteRepository.save(organizationInvite)
        sendMailInvitationToJoinOrganization(request.email, invitedToOrganization)
        return savedInvite
    }

    @Transactional
    override fun revokeInvitation(organizationId: Int, email: String) {
        inviteRepository.findByOrganizationIdAndEmail(organizationId, email).ifPresent {
            inviteRepository.delete(it)
        }
    }

    @Transactional(readOnly = true)
    override fun getAllInvitationsForUser(email: String): List<OrganizationInvitation> {
        return inviteRepository.findByEmail(email)
    }

    @Transactional
    override fun answerToInvitation(request: OrganizationInviteAnswerRequest) {
        inviteRepository.findByOrganizationIdAndEmail(request.organizationId, request.email).ifPresent {
            if (request.join) {
                val role = OrganizationRoleType.fromInt(it.role.id)
                        ?: throw ResourceNotFoundException(ErrorCode.USER_ROLE_MISSING,
                                "Missing role wiht id: ${it.role.id}")
                organizationService.addUserToOrganization(request.userUuid, it.organizationId, role)
            }
            inviteRepository.delete(it)
        }
    }

    @Transactional
    override fun followOrganization(userUuid: UUID, organizationId: Int): OrganizationFollower {
        ServiceUtils.wrapOptional(followerRepository.findByUserUuidAndOrganizationId(userUuid, organizationId))?.let {
            return it
        }
        val follower = OrganizationFollower::class.java.getConstructor().newInstance()
        follower.userUuid = userUuid
        follower.organizationId = organizationId
        follower.createdAt = ZonedDateTime.now()
        return followerRepository.save(follower)
    }

    @Transactional
    override fun unfollowOrganization(userUuid: UUID, organizationId: Int) {
        ServiceUtils.wrapOptional(followerRepository.findByUserUuidAndOrganizationId(userUuid, organizationId))?.let {
            followerRepository.delete(it)
        }
    }

    private fun sendMailInvitationToJoinOrganization(email: String, invitedTo: Organization) {
        logger.debug { "Sending invitation mail to user: $email for organization: ${invitedTo.name}" }
        mailService.sendOrganizationInvitationMail(email, invitedTo.name)
    }

    private fun getRole(role: OrganizationRoleType): Role {
        return when (role) {
            OrganizationRoleType.ORG_ADMIN -> adminRole
            OrganizationRoleType.ORG_MEMBER -> memberRole
        }
    }
}
