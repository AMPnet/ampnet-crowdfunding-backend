package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.config.auth.UserPrincipal
import com.ampnet.crowdfundingbackend.controller.pojo.request.ProjectRequest
import com.ampnet.crowdfundingbackend.controller.pojo.response.ProjectListResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.ProjectResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.ProjectWithFundingResponse
import com.ampnet.crowdfundingbackend.enums.OrganizationPrivilegeType
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.service.OrganizationService
import com.ampnet.crowdfundingbackend.service.ProjectService
import com.ampnet.crowdfundingbackend.service.UserService
import com.ampnet.crowdfundingbackend.service.WalletService
import com.ampnet.crowdfundingbackend.service.pojo.CreateProjectServiceRequest
import mu.KLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
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
        projectService.getProjectById(id)?.let { project ->
            logger.debug { "Project found: ${project.id}" }

            val currentFunding = if (project.wallet == null) {
                logger.info { "Project $id does not have a wallet" }
                // TODO: rethink what to return if the wallet is missing
                0
            } else {
                walletService.getWalletBalance(project.wallet!!)
            }
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
        val user = getUserFromSecurityContext()

        organizationService.getOrganizationMemberships(request.organizationId).find { it.userId == user.id }?.let {
            return if (it.getPrivileges().contains(OrganizationPrivilegeType.PW_PROJECT)) {
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

    private fun createProject(request: ProjectRequest, user: User): ProjectWithFundingResponse {
        val organization = getOrganization(request.organizationId)
        val serviceRequest = CreateProjectServiceRequest(request, organization, user)
        val project = projectService.createProject(serviceRequest)

        // TODO: is it safe to return zero?
        val funding: Long = 0
        return ProjectWithFundingResponse(project, funding)
    }

    private fun getUserFromSecurityContext(): User {
        val userPrincipal = SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        return userService.find(userPrincipal.email)
                ?: throw ResourceNotFoundException(ErrorCode.USER_MISSING,
                        "Missing user with email: ${userPrincipal.email}")
    }

    private fun getOrganization(organizationId: Int): Organization =
            organizationService.findOrganizationById(organizationId)
                    ?: throw ResourceNotFoundException(
                            ErrorCode.ORG_MISSING, "Missing organization with id: $organizationId")
}
