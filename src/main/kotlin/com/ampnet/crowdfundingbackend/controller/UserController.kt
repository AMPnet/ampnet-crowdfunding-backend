package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.request.RoleRequest
import com.ampnet.crowdfundingbackend.controller.pojo.request.UserUpdateRequest
import com.ampnet.crowdfundingbackend.controller.pojo.response.OrganizationInviteResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.OrganizationInvitesListResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.UserResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.UsersListResponse
import com.ampnet.crowdfundingbackend.service.OrganizationService
import com.ampnet.crowdfundingbackend.service.UserService
import mu.KLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
class UserController(private val userService: UserService, private val organizationService: OrganizationService) {

    companion object : KLogging()

    @GetMapping("/me/invites")
    @PreAuthorize("hasAuthority(T(com.ampnet.crowdfundingbackend.enums.PrivilegeType).PRO_ORG_INVITE)")
    fun getMyInvitations(): ResponseEntity<OrganizationInvitesListResponse> {
        logger.debug { "Received request to list my invites" }
        val userId = getUserId()
        val invites = organizationService.getAllOrganizationInvitesForUser(userId)
            .map { OrganizationInviteResponse(it) }
        return ResponseEntity.ok(OrganizationInvitesListResponse(invites))
    }

    @PostMapping("/me/invites/{organizationId}/accept")
    @PreAuthorize("hasAuthority(T(com.ampnet.crowdfundingbackend.enums.PrivilegeType).PWO_ORG_INVITE)")
    fun acceptOrganizationInvitation(@PathVariable("organizationId") organizationId: Int): ResponseEntity<Unit> {
        logger.debug { "Received request accept organization invite for organization: $organizationId" }
        val userId = getUserId()
        organizationService.answerToOrganizationInvitation(userId, true, organizationId)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/me/invites/{organizationId}/reject")
    @PreAuthorize("hasAuthority(T(com.ampnet.crowdfundingbackend.enums.PrivilegeType).PWO_ORG_INVITE)")
    fun rejectOrganizationInvitation(@PathVariable("organizationId") organizationId: Int): ResponseEntity<Unit> {
        logger.debug { "Received request reject organization invite for organization: $organizationId" }
        val userId = getUserId()
        organizationService.answerToOrganizationInvitation(userId, false, organizationId)
        return ResponseEntity.ok().build()
    }

    private fun getUserId(): Int = ControllerUtils.getUserFromSecurityContext(userService).id
}
