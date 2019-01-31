package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.request.ProjectRequest
import com.ampnet.crowdfundingbackend.controller.pojo.response.DocumentResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.ProjectListResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.ProjectResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.ProjectWithFundingResponse
import com.ampnet.crowdfundingbackend.enums.OrganizationPrivilegeType
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.OrganizationMembership
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.service.OrganizationService
import com.ampnet.crowdfundingbackend.service.ProjectService
import com.ampnet.crowdfundingbackend.service.UserService
import com.ampnet.crowdfundingbackend.service.WalletService
import com.ampnet.crowdfundingbackend.service.pojo.CreateProjectServiceRequest
import com.ampnet.crowdfundingbackend.service.pojo.DocumentSaveRequest
import mu.KLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import javax.validation.Valid

@RestController
class ProjectController(
    private val projectService: ProjectService,
    private val walletService: WalletService,
    private val userService: UserService,
    private val organizationService: OrganizationService
) {

    companion object : KLogging()

    @GetMapping("/project/{id}")
    fun getProject(@PathVariable id: Int): ResponseEntity<ProjectWithFundingResponse> {
        logger.debug { "Received request to get project with id: $id" }

        // TODO: add document to response
        projectService.getProjectByIdWithAllData(id)?.let { project ->
            logger.debug { "Project found: ${project.id}" }

            val currentFunding = getCurrentFundingForProject(project)
            logger.debug { "Project $id current funding is: $currentFunding" }

            val response = ProjectWithFundingResponse(project, currentFunding)
            return ResponseEntity.ok(response)
        }
        logger.info { "Project with id: $id not found" }
        return ResponseEntity.notFound().build()
    }

    @PostMapping("/project")
    fun createProject(@RequestBody @Valid request: ProjectRequest): ResponseEntity<ProjectWithFundingResponse> {
        logger.debug { "Received request to create project: $request" }
        val user = ControllerUtils.getUserFromSecurityContext(userService)

        getUserMembershipInOrganization(user.id, request.organizationId)?.let {
            return if (hasPrivilegeToWriteProject(it)) {
                val project = createProject(request, user)
                ResponseEntity.ok(project)
            } else {
                logger.info { "User does not have organization privilege to write users: PW_PROJECT" }
                ResponseEntity.status(HttpStatus.FORBIDDEN).build()
            }
        }
        logger.info { "User ${user.id} is not a member of organization ${request.organizationId}" }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
    }

    @GetMapping("/project/organization/{organizationId}")
    fun getAllProjectForOrganization(@PathVariable organizationId: Int): ResponseEntity<ProjectListResponse> {
        logger.debug { "Received request to get all projects for organization: $organizationId" }
        val projects = projectService.getAllProjectsForOrganization(organizationId).map { ProjectResponse(it) }
        return ResponseEntity.ok(ProjectListResponse(projects))
    }

    @PostMapping("/project/{projectId}/document")
    fun addDocument(
        @PathVariable("projectId") projectId: Int,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<DocumentResponse> {
        logger.debug { "Received request to add document to project: $projectId" }
        val user = ControllerUtils.getUserFromSecurityContext(userService)

        val project = projectService.getProjectById(projectId)
                ?: throw ResourceNotFoundException(ErrorCode.PRJ_MISSING, "Missing project: $projectId")

        getUserMembershipInOrganization(user.id, project.organization.id)?.let {
            return if (hasPrivilegeToWriteProject(it)) {
                val request = DocumentSaveRequest(file, user)
                val document = projectService.addDocument(project.id, request)
                return ResponseEntity.ok(DocumentResponse(document))
            } else {
                logger.info { "User does not have organization privilege to write users: PW_PROJECT" }
                ResponseEntity.status(HttpStatus.FORBIDDEN).build()
            }
        }
        logger.info { "User ${user.id} is not a member of organization ${project.organization.id}" }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
    }

    private fun createProject(request: ProjectRequest, user: User): ProjectWithFundingResponse {
        val organization = getOrganization(request.organizationId)
        val serviceRequest = CreateProjectServiceRequest(request, organization, user)
        val project = projectService.createProject(serviceRequest)

        return ProjectWithFundingResponse(project, null)
    }

    private fun getCurrentFundingForProject(project: Project): Long? {
        project.wallet?.let {
            return walletService.getWalletBalance(it)
        }
        logger.info { "Project ${project.id} does not have a wallet" }
        return null
    }

    private fun getOrganization(organizationId: Int): Organization =
            organizationService.findOrganizationById(organizationId)
                    ?: throw ResourceNotFoundException(
                            ErrorCode.ORG_MISSING, "Missing organization with id: $organizationId")

    private fun getUserMembershipInOrganization(userId: Int, organizationId: Int): OrganizationMembership? =
            organizationService.getOrganizationMemberships(organizationId).find { it.userId == userId }

    private fun hasPrivilegeToWriteProject(membership: OrganizationMembership): Boolean =
            membership.getPrivileges().contains(OrganizationPrivilegeType.PW_PROJECT)
}
