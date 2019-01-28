package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.config.auth.UserPrincipal
import com.ampnet.crowdfundingbackend.controller.pojo.request.OrganizationInviteRequest
import com.ampnet.crowdfundingbackend.controller.pojo.request.OrganizationRequest
import com.ampnet.crowdfundingbackend.controller.pojo.response.OrganizationListResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.OrganizationResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.OrganizationUserResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.OrganizationUsersListResponse
import com.ampnet.crowdfundingbackend.enums.OrganizationPrivilegeType
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.persistence.model.OrganizationMembership
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.service.OrganizationService
import com.ampnet.crowdfundingbackend.service.UserService
import com.ampnet.crowdfundingbackend.service.pojo.OrganizationInviteServiceRequest
import com.ampnet.crowdfundingbackend.service.pojo.OrganizationServiceRequest
import mu.KLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
class OrganizationController(
    private val organizationService: OrganizationService,
    private val userService: UserService
) {

    companion object : KLogging()

    @GetMapping("/organization")
    @PreAuthorize("hasAuthority(T(com.ampnet.crowdfundingbackend.enums.PrivilegeType).PRA_ORG)")
    fun getOrganizations(): ResponseEntity<OrganizationListResponse> {
        logger.debug { "Received request for all organizations" }
        val organizations = organizationService.getAllOrganizations().map { OrganizationResponse(it) }
        return ResponseEntity.ok(OrganizationListResponse(organizations))
    }

    @GetMapping("/organization/personal")
    fun getPersonalOrganizations(): ResponseEntity<OrganizationListResponse> {
        logger.debug { "Received request for personal organizations" }
        val user = getUserFromSecurityContext()
        val organizations = organizationService.findAllOrganizationsForUser(user.id).map { OrganizationResponse(it) }
        return ResponseEntity.ok(OrganizationListResponse(organizations))
    }

    @GetMapping("/organization/{id}")
    fun getOrganization(@PathVariable("id") id: Int): ResponseEntity<OrganizationResponse> {
        logger.debug { "Received request for organization with id: $id" }
        organizationService.findOrganizationById(id)?.let {
            return ResponseEntity.ok(OrganizationResponse(it))
        }
        return ResponseEntity.notFound().build()
    }

    @PostMapping("/organization")
    fun createOrganization(@RequestBody @Valid request: OrganizationRequest): ResponseEntity<OrganizationResponse> {
        logger.debug { "Received request to create organization: $request" }
        val user = getUserFromSecurityContext()

        val serviceRequest = OrganizationServiceRequest(request, user)
        val organization = organizationService.createOrganization(serviceRequest)
        return ResponseEntity.ok(OrganizationResponse(organization))
    }

    @PostMapping("/organization/{id}/approve")
    @PreAuthorize("hasAuthority(T(com.ampnet.crowdfundingbackend.enums.PrivilegeType).PWA_ORG_APPROVE)")
    fun approveOrganization(@PathVariable("id") id: Int): ResponseEntity<OrganizationResponse> {
        val user = getUserFromSecurityContext()
        logger.debug { "Received request to approve organization with id: $id by user: ${user.email}" }

        val organization = organizationService.approveOrganization(id, true, user)
        return ResponseEntity.ok(OrganizationResponse(organization))
    }

    @GetMapping("/organization/{id}/users")
    fun getOrganizationUsers(@PathVariable("id") id: Int): ResponseEntity<OrganizationUsersListResponse> {
        logger.debug { "Received request to get all users for organization: $id" }
        val user = getUserFromSecurityContext()

        organizationService.getOrganizationMemberships(id).find { it.userId == user.id }?.let {
            return if (hasPrivilegeToSeeOrganizationUsers(it)) {
                val users = organizationService.findAllUsersFromOrganization(id).map {
                    user -> OrganizationUserResponse(user)
                }
                ResponseEntity.ok(OrganizationUsersListResponse(users))
            } else {
                logger.info { "User does not have organization privilege to read users: PR_USERS" }
                ResponseEntity.status(HttpStatus.FORBIDDEN).build()
            }
        }

        logger.info { "User ${user.id} is not a member of organization $id" }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
    }

    @PostMapping("/organization/{id}/invite")
    fun inviteToOrganization(
        @PathVariable("id") id: Int,
        @RequestBody @Valid request: OrganizationInviteRequest
    ): ResponseEntity<Unit> {
        val user = getUserFromSecurityContext()
        logger.debug { "Received request to invited user to organization $id by user: ${user.email}" }

        return ifUserHasPrivilegeWriteUserInOrganizationThenDo(user.id, id) {
            val serviceRequest = OrganizationInviteServiceRequest(request, id, user)
            organizationService.inviteUserToOrganization(serviceRequest)
        }
    }

    @PostMapping("/organization/{organizationId}/invite/{revokeUserId}/revoke")
    fun revokeInvitationToOrganization(
        @PathVariable("organizationId") organizationId: Int,
        @PathVariable("revokeUserId") revokeUserId: Int
    ): ResponseEntity<Unit> {
        val user = getUserFromSecurityContext()
        logger.debug { "Received request to invited user to organization $organizationId by user: ${user.email}" }

        return ifUserHasPrivilegeWriteUserInOrganizationThenDo(user.id, organizationId) {
            organizationService.revokeInvitationToJoinOrganization(organizationId, revokeUserId)
        }
    }

    private fun ifUserHasPrivilegeWriteUserInOrganizationThenDo(
        userId: Int,
        organizationId: Int,
        action: () -> (Unit)
    ): ResponseEntity<Unit> {
        organizationService.getOrganizationMemberships(organizationId).find { it.userId == userId }?.let {
            return if (hasPrivilegeToWriteOrganizationUsers(it)) {
                action()
                return ResponseEntity.ok().build()
            } else {
                logger.info { "User does not have organization privilege to write users: PW_USERS" }
                ResponseEntity.status(HttpStatus.FORBIDDEN).build()
            }
        }
        logger.info { "User $userId is not a member of organization $organizationId" }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
    }

    private fun getUserFromSecurityContext(): User {
        val userPrincipal = SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        return userService.find(userPrincipal.email)
                ?: throw ResourceNotFoundException(ErrorCode.USER_MISSING,
                        "Missing user with email: ${userPrincipal.email}")
    }

    private fun hasPrivilegeToSeeOrganizationUsers(membership: OrganizationMembership): Boolean =
        membership.getPrivileges().contains(OrganizationPrivilegeType.PR_USERS)

    private fun hasPrivilegeToWriteOrganizationUsers(membership: OrganizationMembership): Boolean =
            membership.getPrivileges().contains(OrganizationPrivilegeType.PW_USERS)
}
