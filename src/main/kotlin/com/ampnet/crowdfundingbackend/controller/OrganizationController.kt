package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.request.OrganizationInviteRequest
import com.ampnet.crowdfundingbackend.controller.pojo.request.OrganizationRequest
import com.ampnet.crowdfundingbackend.controller.pojo.response.DocumentResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.OrganizationListResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.OrganizationResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.OrganizationWithDocumentResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.OrganizationUserResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.OrganizationUsersListResponse
import com.ampnet.crowdfundingbackend.service.OrganizationService
import com.ampnet.crowdfundingbackend.service.UserService
import com.ampnet.crowdfundingbackend.service.pojo.DocumentSaveRequest
import com.ampnet.crowdfundingbackend.service.pojo.OrganizationInviteServiceRequest
import com.ampnet.crowdfundingbackend.service.pojo.OrganizationServiceRequest
import mu.KLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
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
        val user = ControllerUtils.getUserFromSecurityContext(userService)
        val organizations = organizationService.findAllOrganizationsForUser(user.id).map { OrganizationResponse(it) }
        return ResponseEntity.ok(OrganizationListResponse(organizations))
    }

    @GetMapping("/organization/{id}")
    fun getOrganization(@PathVariable("id") id: Int): ResponseEntity<OrganizationWithDocumentResponse> {
        logger.debug { "Received request for organization with id: $id" }
        organizationService.findOrganizationById(id)?.let {
            return ResponseEntity.ok(OrganizationWithDocumentResponse(it))
        }
        return ResponseEntity.notFound().build()
    }

    @PostMapping("/organization")
    fun createOrganization(
        @RequestBody @Valid request: OrganizationRequest
    ): ResponseEntity<OrganizationWithDocumentResponse> {
        logger.debug { "Received request to create organization: $request" }
        val user = ControllerUtils.getUserFromSecurityContext(userService)

        val serviceRequest = OrganizationServiceRequest(request, user)
        val organization = organizationService.createOrganization(serviceRequest)
        return ResponseEntity.ok(OrganizationWithDocumentResponse(organization))
    }

    @PostMapping("/organization/{id}/approve")
    @PreAuthorize("hasAuthority(T(com.ampnet.crowdfundingbackend.enums.PrivilegeType).PWA_ORG_APPROVE)")
    fun approveOrganization(@PathVariable("id") id: Int): ResponseEntity<OrganizationWithDocumentResponse> {
        val user = ControllerUtils.getUserFromSecurityContext(userService)
        logger.debug { "Received request to approve organization with id: $id by user: ${user.email}" }

        val organization = organizationService.approveOrganization(id, true, user)
        return ResponseEntity.ok(OrganizationWithDocumentResponse(organization))
    }

    @GetMapping("/organization/{id}/users")
    fun getOrganizationUsers(@PathVariable("id") id: Int): ResponseEntity<OrganizationUsersListResponse> {
        logger.debug { "Received request to get all users for organization: $id" }
        val user = ControllerUtils.getUserFromSecurityContext(userService)

        return ifUserHasPrivilegeWriteUserInOrganizationThenReturn(user.id, id) {
            val users = organizationService.findAllUsersFromOrganization(id).map {
                user -> OrganizationUserResponse(user)
            }
            OrganizationUsersListResponse(users)
        }
    }

    @PostMapping("/organization/{id}/invite")
    fun inviteToOrganization(
        @PathVariable("id") id: Int,
        @RequestBody @Valid request: OrganizationInviteRequest
    ): ResponseEntity<Unit> {
        val user = ControllerUtils.getUserFromSecurityContext(userService)
        logger.debug { "Received request to invited user to organization $id by user: ${user.email}" }

        return ifUserHasPrivilegeWriteUserInOrganizationThenReturn(user.id, id) {
            val serviceRequest = OrganizationInviteServiceRequest(request, id, user)
            organizationService.inviteUserToOrganization(serviceRequest)
            Unit
        }
    }

    @PostMapping("/organization/{organizationId}/invite/{revokeUserId}/revoke")
    fun revokeInvitationToOrganization(
        @PathVariable("organizationId") organizationId: Int,
        @PathVariable("revokeUserId") revokeUserId: Int
    ): ResponseEntity<Unit> {
        val user = ControllerUtils.getUserFromSecurityContext(userService)
        logger.debug { "Received request to invited user to organization $organizationId by user: ${user.email}" }

        return ifUserHasPrivilegeWriteUserInOrganizationThenReturn(user.id, organizationId) {
            organizationService.revokeInvitationToJoinOrganization(organizationId, revokeUserId)
        }
    }

    @PostMapping("/organization/{organizationId}/document")
    fun addDocument(
        @PathVariable("organizationId") organizationId: Int,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<DocumentResponse> {
        logger.debug { "Received request to add document: ${file.name} to organization: $organizationId" }
        val user = ControllerUtils.getUserFromSecurityContext(userService)

        return ifUserHasPrivilegeWriteUserInOrganizationThenReturn(user.id, organizationId) {
            val documentSaveRequest = DocumentSaveRequest(file, user)
            val document = organizationService.addDocument(organizationId, documentSaveRequest)
            DocumentResponse(document)
        }
    }

    @DeleteMapping("/organization/{organizationId}/document/{documentId}")
    fun removeDocument(
        @PathVariable("organizationId") organizationId: Int,
        @PathVariable("documentId") documentId: Int
    ): ResponseEntity<Unit> {
        logger.debug { "Received request to delete document: $documentId for organization $organizationId" }
        val user = ControllerUtils.getUserFromSecurityContext(userService)

        return ifUserHasPrivilegeWriteUserInOrganizationThenReturn(user.id, organizationId) {
            organizationService.removeDocument(organizationId, documentId)
        }
    }

    private fun <T> ifUserHasPrivilegeWriteUserInOrganizationThenReturn(
        userId: Int,
        organizationId: Int,
        action: () -> (T)
    ): ResponseEntity<T> {
        organizationService.getOrganizationMemberships(organizationId)
                .find { it.userId == userId }
                ?.let { orgMembership ->
                    return if (orgMembership.hasPrivilegeToWriteOrganizationUsers()) {
                        val response = action()
                        ResponseEntity.ok(response)
                    } else {
                        logger.info { "User does not have organization privilege to write users: PW_USERS" }
                        ResponseEntity.status(HttpStatus.FORBIDDEN).build()
                    }
                }
        logger.info { "User $userId is not a member of organization $organizationId" }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
    }
}
