package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.config.auth.UserPrincipal
import com.ampnet.crowdfundingbackend.controller.pojo.request.OrganizationRequest
import com.ampnet.crowdfundingbackend.controller.pojo.response.OrganizationListResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.OrganizationResponse
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.service.OrganizationService
import com.ampnet.crowdfundingbackend.service.UserService
import com.ampnet.crowdfundingbackend.service.pojo.OrganizationServiceRequest
import mu.KLogging
import org.springframework.http.ResponseEntity
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
        logger.debug("Received request for all organizations")
        val organizations = organizationService.getAllOrganizations().map { OrganizationResponse(it) }
        return ResponseEntity.ok(OrganizationListResponse(organizations))
    }

    @GetMapping("/organization/{id}")
    fun getOrganization(@PathVariable("id") id: Int): ResponseEntity<OrganizationResponse> {
        logger.debug("Received request for organization with id: $id")
        organizationService.findOrganizationById(id)?.let {
            return ResponseEntity.ok(OrganizationResponse(it))
        }
        return ResponseEntity.notFound().build()
    }

    @PostMapping("/organization")
    fun createOrganization(@RequestBody request: OrganizationRequest): ResponseEntity<OrganizationResponse> {
        logger.debug("Received request to create organization: $request")
        val userPrincipal = SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        val user = getUserFromEmail(userPrincipal.email)

        // TODO: use ipfs client and get document hashes
        val documentHashes = emptyList<String>()

        val serviceRequest = OrganizationServiceRequest(request, user, documentHashes)
        val organization = organizationService.createOrganization(serviceRequest)
        return ResponseEntity.ok(OrganizationResponse(organization))
    }

    private fun getUserFromEmail(email: String): User {
        return userService.find(email) ?: throw ResourceNotFoundException("Missing user with email: $email")
    }
}
