package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.config.auth.UserPrincipal
import com.ampnet.crowdfundingbackend.controller.pojo.request.OrganizationRequest
import com.ampnet.crowdfundingbackend.controller.pojo.response.OrganizationListResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.OrganizationResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.OrganizationUserResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.OrganizationUsersListResponse
import com.ampnet.crowdfundingbackend.enums.OrganizationPrivilegeType
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.persistence.model.OrganizationMembership
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.service.OrganizationService
import com.ampnet.crowdfundingbackend.service.UserService
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

@RestController
class OrganizationController(
    private val organizationService: OrganizationService,
    private val userService: UserService
) {

    companion object : KLogging()

    @GetMapping("/organization")
    fun getOrganizations(): ResponseEntity<OrganizationListResponse> {
        logger.debug { "Received request for all organizations" }
        val organizations = organizationService.getAllOrganizations().map { OrganizationResponse(it) }
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
    fun createOrganization(@RequestBody request: OrganizationRequest): ResponseEntity<OrganizationResponse> {
        logger.debug { "Received request to create organization: $request" }
        val userPrincipal = SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        val user = getUserFromEmail(userPrincipal.email)

        // TODO: use ipfs client and get document hashes
        val documentHashes = emptyList<String>()

        val serviceRequest = OrganizationServiceRequest(request, user, documentHashes)
        val organization = organizationService.createOrganization(serviceRequest)
        return ResponseEntity.ok(OrganizationResponse(organization))
    }

    @PostMapping("/organization/{id}/approve")
    @PreAuthorize("hasAuthority(T(com.ampnet.crowdfundingbackend.enums.PrivilegeType).PWA_ORG)")
    fun approveOrganization(@PathVariable("id") id: Int): ResponseEntity<OrganizationResponse> {
        val userPrincipal = SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        logger.debug { "Received request to approve organization with id: $id by user: ${userPrincipal.email}" }

        val user = getUserFromEmail(userPrincipal.email)
        val organization = organizationService.approveOrganization(id, true, user)
        return ResponseEntity.ok(OrganizationResponse(organization))
    }

    @GetMapping("/organization/{id}/users")
    fun getOrganizationUsers(@PathVariable("id") id: Int): ResponseEntity<OrganizationUsersListResponse> {
        logger.debug { "Received request to get all users for organization: $id" }
        val userPrincipal = SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        val user = getUserFromEmail(userPrincipal.email)

        organizationService.getOrganizationMemberships(id).find { it.userId == user.id }?.let {
            return if (hasPrivilegeToSeeOrganizationUsers(it)) {
                val users = organizationService.findAllUsersFromOrganization(id).map {
                    user -> OrganizationUserResponse(user)
                }
                ResponseEntity.ok(OrganizationUsersListResponse(users))
            } else {
                logger.info { "User does not have organization privilege to read users: PR_USERS" }
                ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
            }
        }

        logger.info { "User ${user.id} is not a member of organization $id" }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
    }

    private fun getUserFromEmail(email: String): User =
        userService.find(email) ?: throw ResourceNotFoundException("Missing user with email: $email")

    private fun hasPrivilegeToSeeOrganizationUsers(membership: OrganizationMembership): Boolean =
        membership.getPrivileges().contains(OrganizationPrivilegeType.PR_USERS)
}
