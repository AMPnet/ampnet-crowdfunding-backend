package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.request.OrganizationRequest
import com.ampnet.crowdfundingbackend.controller.pojo.response.DocumentResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.OrganizationListResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.OrganizationMembershipResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.OrganizationMembershipsResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.OrganizationResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.OrganizationWithDocumentResponse
import com.ampnet.crowdfundingbackend.service.OrganizationService
import com.ampnet.crowdfundingbackend.service.pojo.DocumentSaveRequest
import com.ampnet.crowdfundingbackend.service.pojo.OrganizationServiceRequest
import com.ampnet.crowdfundingbackend.userservice.UserService
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
import java.util.UUID
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
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        val organizations = organizationService
                .findAllOrganizationsForUser(userPrincipal.uuid)
                .map { OrganizationResponse(it) }
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
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()

        val serviceRequest = OrganizationServiceRequest(request, userPrincipal.uuid)
        val organization = organizationService.createOrganization(serviceRequest)
        return ResponseEntity.ok(OrganizationWithDocumentResponse(organization))
    }

    @PostMapping("/organization/{id}/approve")
    @PreAuthorize("hasAuthority(T(com.ampnet.crowdfundingbackend.enums.PrivilegeType).PWA_ORG_APPROVE)")
    fun approveOrganization(@PathVariable("id") id: Int): ResponseEntity<OrganizationWithDocumentResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to approve organization with id: $id by user: ${userPrincipal.email}" }

        val organization = organizationService.approveOrganization(id, true, userPrincipal.uuid)
        return ResponseEntity.ok(OrganizationWithDocumentResponse(organization))
    }

    @GetMapping("/organization/{organizationId}/members")
    fun getOrganizationMembers(
        @PathVariable("organizationId") organizationId: Int
    ): ResponseEntity<OrganizationMembershipsResponse> {
        logger.debug { "Received request to get members for organization: $organizationId" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()

        return ifUserHasPrivilegeWriteUserInOrganizationThenReturn(userPrincipal.uuid, organizationId) {
            val members = organizationService.getOrganizationMemberships(organizationId)
            val membersWithoutMe = members.filter { userPrincipal.uuid != it.userUuid }
            val users = userService.getUsers(membersWithoutMe.map { it.userUuid })

            val membersResponse = membersWithoutMe.map {
                OrganizationMembershipResponse(it, users.firstOrNull { user -> user.uuid == it.userUuid.toString() })
            }
            OrganizationMembershipsResponse(membersResponse)
        }
    }

    @DeleteMapping("/organization/{organizationId}/members/{memberUuid}")
    fun deleteOrganizationMember(
        @PathVariable("organizationId") organizationId: Int,
        @PathVariable("memberUuid") memberUuid: UUID
    ): ResponseEntity<Unit> {
        logger.debug { "Received request to remove member: $memberUuid from organization: $organizationId" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()

        return ifUserHasPrivilegeWriteUserInOrganizationThenReturn(userPrincipal.uuid, organizationId) {
            organizationService.removeUserFromOrganization(memberUuid, organizationId)
        }
    }

    @PostMapping("/organization/{organizationId}/document")
    fun addDocument(
        @PathVariable("organizationId") organizationId: Int,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<DocumentResponse> {
        logger.debug { "Received request to add document: ${file.name} to organization: $organizationId" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()

        return ifUserHasPrivilegeToWriteOrganizationThenReturn(userPrincipal.uuid, organizationId) {
            val documentSaveRequest = DocumentSaveRequest(file, userPrincipal.uuid)
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
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()

        return ifUserHasPrivilegeToWriteOrganizationThenReturn(userPrincipal.uuid, organizationId) {
            organizationService.removeDocument(organizationId, documentId)
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

    private fun <T> ifUserHasPrivilegeToWriteOrganizationThenReturn(
        userUuid: UUID,
        organizationId: Int,
        action: () -> (T)
    ): ResponseEntity<T> {
        organizationService.getOrganizationMemberships(organizationId)
                .find { it.userUuid == userUuid }
                ?.let { orgMembership ->
                    return if (orgMembership.hasPrivilegeToWriteOrganization()) {
                        val response = action()
                        ResponseEntity.ok(response)
                    } else {
                        logger.info { "User does not have organization privilege: PW_ORG" }
                        ResponseEntity.status(HttpStatus.FORBIDDEN).build()
                    }
                }
        logger.info { "User $userUuid is not a member of organization $organizationId" }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
    }
}
