package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.request.OrganizationInviteRequest
import com.ampnet.crowdfundingbackend.controller.pojo.response.OrganizationInviteResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.OrganizationInvitesListResponse
import com.ampnet.crowdfundingbackend.service.OrganizationInviteService
import com.ampnet.crowdfundingbackend.service.OrganizationService
import com.ampnet.crowdfundingbackend.service.pojo.OrganizationInviteAnswerRequest
import com.ampnet.crowdfundingbackend.service.pojo.OrganizationInviteServiceRequest
import mu.KLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import javax.validation.Valid

@RestController
class OrganizationInvitationController(
    private val organizationInviteService: OrganizationInviteService,
    private val organizationService: OrganizationService
) {

    companion object : KLogging()

    @GetMapping("/invites/me")
    @PreAuthorize("hasAuthority(T(com.ampnet.crowdfundingbackend.enums.PrivilegeType).PRO_ORG_INVITE)")
    fun getMyInvitations(): ResponseEntity<OrganizationInvitesListResponse> {
        logger.debug { "Received request to list my invites" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        val invites = organizationInviteService.getAllInvitationsForUser(userPrincipal.email)
            .map { OrganizationInviteResponse(it) }
        return ResponseEntity.ok(OrganizationInvitesListResponse(invites))
    }

    @PostMapping("/invites/me/{organizationId}/accept")
    @PreAuthorize("hasAuthority(T(com.ampnet.crowdfundingbackend.enums.PrivilegeType).PWO_ORG_INVITE)")
    fun acceptOrganizationInvitation(@PathVariable("organizationId") organizationId: Int): ResponseEntity<Unit> {
        logger.debug { "Received request accept organization invite for organization: $organizationId" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        val request = OrganizationInviteAnswerRequest(userPrincipal.uuid, userPrincipal.email, true, organizationId)
        organizationInviteService.answerToInvitation(request)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/invites/me/{organizationId}/reject")
    @PreAuthorize("hasAuthority(T(com.ampnet.crowdfundingbackend.enums.PrivilegeType).PWO_ORG_INVITE)")
    fun rejectOrganizationInvitation(@PathVariable("organizationId") organizationId: Int): ResponseEntity<Unit> {
        logger.debug { "Received request reject organization invite for organization: $organizationId" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        val request = OrganizationInviteAnswerRequest(userPrincipal.uuid, userPrincipal.email, false, organizationId)
        organizationInviteService.answerToInvitation(request)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/invites/organization/{id}/invite")
    fun inviteToOrganization(
        @PathVariable("id") id: Int,
        @RequestBody @Valid request: OrganizationInviteRequest
    ): ResponseEntity<Unit> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to invited user to organization $id by user: ${userPrincipal.email}" }

        return ifUserHasPrivilegeWriteUserInOrganizationThenReturn(userPrincipal.uuid, id) {
            val serviceRequest = OrganizationInviteServiceRequest(request, id, userPrincipal.uuid)
            organizationInviteService.sendInvitation(serviceRequest)
            Unit
        }
    }

    @PostMapping("/invites/organization/{organizationId}/{revokeEmail}/revoke")
    fun revokeInvitationToOrganization(
        @PathVariable("organizationId") organizationId: Int,
        @PathVariable("revokeEmail") revokeEmail: String
    ): ResponseEntity<Unit> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug {
            "Received request to invited user to organization $organizationId by user: ${userPrincipal.email}"
        }
        return ifUserHasPrivilegeWriteUserInOrganizationThenReturn(userPrincipal.uuid, organizationId) {
            organizationInviteService.revokeInvitation(organizationId, revokeEmail)
        }
    }

    private fun <T> ifUserHasPrivilegeWriteUserInOrganizationThenReturn(
        userUuid: UUID,
        organizationId: Int,
        action: () -> (T)
    ): ResponseEntity<T> {
        organizationService.getOrganizationMemberships(organizationId)
                .find { it.userUuid == userUuid }
                ?.let { orgMembership ->
                    return if (orgMembership.hasPrivilegeToWriteOrganizationUsers()) {
                        val response = action()
                        ResponseEntity.ok(response)
                    } else {
                        logger.info { "User does not have organization privilege to write users: PW_USERS" }
                        ResponseEntity.status(HttpStatus.FORBIDDEN).build()
                    }
                }
        logger.info { "User $userUuid is not a member of organization $organizationId" }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
    }
}
