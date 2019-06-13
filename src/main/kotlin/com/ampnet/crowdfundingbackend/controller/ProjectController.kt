package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.request.ImageLinkListRequest
import com.ampnet.crowdfundingbackend.controller.pojo.request.ProjectRequest
import com.ampnet.crowdfundingbackend.controller.pojo.response.DocumentResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.ProjectListResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.ProjectResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.ProjectWithFundingResponse
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.OrganizationMembership
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.service.OrganizationService
import com.ampnet.crowdfundingbackend.service.ProjectInvestmentService
import com.ampnet.crowdfundingbackend.service.ProjectService
import com.ampnet.crowdfundingbackend.service.UserService
import com.ampnet.crowdfundingbackend.service.WalletService
import com.ampnet.crowdfundingbackend.service.pojo.CreateProjectServiceRequest
import com.ampnet.crowdfundingbackend.service.pojo.DocumentSaveRequest
import mu.KLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
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
class ProjectController(
    private val projectService: ProjectService,
    private val walletService: WalletService,
    private val userService: UserService,
    private val organizationService: OrganizationService,
    private val projectInvestmentService: ProjectInvestmentService
) {

    companion object : KLogging()

    @GetMapping("/project/{id}")
    fun getProject(@PathVariable id: Int): ResponseEntity<ProjectWithFundingResponse> {
        logger.debug { "Received request to get project with id: $id" }

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
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        val user = ControllerUtils.getUserFromSecurityContext(userService)

        return ifUserHasPrivilegeToWriteInProjectThenReturn(userPrincipal.uuid, request.organizationId) {
            createProject(request, user)
        }
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
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        val project = getProjectById(projectId)

        return ifUserHasPrivilegeToWriteInProjectThenReturn(userPrincipal.uuid, project.organization.id) {
            val request = DocumentSaveRequest(file, userPrincipal.uuid)
            val document = projectService.addDocument(project.id, request)
            DocumentResponse(document)
        }
    }

    @DeleteMapping("/project/{projectId}/document/{documentId}")
    fun removeDocument(
        @PathVariable("projectId") projectId: Int,
        @PathVariable("documentId") documentId: Int
    ): ResponseEntity<Unit> {
        logger.debug { "Received request to delete document: $documentId for project $projectId" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        val project = getProjectById(projectId)

        return ifUserHasPrivilegeToWriteInProjectThenReturn(userPrincipal.uuid, project.organization.id) {
            projectService.removeDocument(projectId, documentId)
        }
    }

    @PostMapping("/project/{projectId}/image/main")
    fun addMainImage(
        @PathVariable("projectId") projectId: Int,
        @RequestParam("image") image: MultipartFile
    ): ResponseEntity<Unit> {
        logger.debug { "Received request to add main image to project: $projectId" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        val project = getProjectById(projectId)

        return ifUserHasPrivilegeToWriteInProjectThenReturn(userPrincipal.uuid, project.organization.id) {
            val imageName = getImageNameFromMultipartFile(image)
            projectService.addMainImage(project, imageName, image.bytes)
        }
    }

    @PostMapping("/project/{projectId}/image/gallery")
    fun addGalleryImage(
        @PathVariable("projectId") projectId: Int,
        @RequestParam("image") image: MultipartFile
    ): ResponseEntity<Unit> {
        logger.debug { "Received request to add gallery image to project: $projectId" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        val project = getProjectById(projectId)

        return ifUserHasPrivilegeToWriteInProjectThenReturn(userPrincipal.uuid, project.organization.id) {
            val imageName = getImageNameFromMultipartFile(image)
            projectService.addImageToGallery(project, imageName, image.bytes)
        }
    }

    @DeleteMapping("/project/{projectId}/image/gallery")
    fun removeImageFromGallery(
        @PathVariable("projectId") projectId: Int,
        @RequestBody request: ImageLinkListRequest
    ): ResponseEntity<Unit> {
        logger.debug { "Received request to delete gallery images for project: $projectId" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        val project = getProjectById(projectId)

        return ifUserHasPrivilegeToWriteInProjectThenReturn(userPrincipal.uuid, project.organization.id) {
            projectService.removeImagesFromGallery(project, request.images)
        }
    }

//    @GetMapping("/project/{projectId}/invest")
//    fun generateInvestTransaction(
//        @PathVariable("projectId") projectId: Int,
//        @RequestParam(name = "amount") amount: Long
//    ): ResponseEntity<TransactionResponse> {
//        logger.debug { "Received request to generate invest transaction for project: $projectId" }
//        val user = ControllerUtils.getUserFromSecurityContext(userService)
//        val project = getProjectById(projectId)
//
//        val request = ProjectInvestmentRequest(project, user, amount)
//        val transaction = projectInvestmentService.generateInvestInProjectTransaction(request)
//        return ResponseEntity.ok(TransactionResponse(transaction))
//    }

//    @GetMapping("/project/{projectId}/invest/confirm")
//    fun generateConfirmInvestTransaction(
//        @PathVariable("projectId") projectId: Int
//    ): ResponseEntity<TransactionResponse> {
//        logger.debug { "Received request to generate confirm invest transaction for project: $projectId" }
//        val user = ControllerUtils.getUserFromSecurityContext(userService)
//        val project = getProjectById(projectId)
//
//        val transaction = projectInvestmentService.generateConfirmInvestment(user, project)
//        return ResponseEntity.ok(TransactionResponse(transaction))
//    }

    private fun getImageNameFromMultipartFile(multipartFile: MultipartFile): String =
            multipartFile.originalFilename ?: multipartFile.name

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

    private fun getUserMembershipInOrganization(userUuid: String, organizationId: Int): OrganizationMembership? =
            organizationService.getOrganizationMemberships(organizationId).find { it.userUuid == userUuid }

    private fun getProjectById(projectId: Int): Project =
        projectService.getProjectById(projectId)
            ?: throw ResourceNotFoundException(ErrorCode.PRJ_MISSING, "Missing project: $projectId")

    private fun <T> ifUserHasPrivilegeToWriteInProjectThenReturn(
        userUuid: String,
        organizationId: Int,
        action: () -> (T)
    ): ResponseEntity<T> {
        getUserMembershipInOrganization(userUuid, organizationId)?.let { orgMembership ->
            return if (orgMembership.hasPrivilegeToWriteProject()) {
                val response = action()
                ResponseEntity.ok(response)
            } else {
                logger.info { "User does not have organization privilege to write users: PW_PROJECT" }
                ResponseEntity.status(HttpStatus.FORBIDDEN).build()
            }
        }
        logger.info { "User $userUuid is not a member of organization $organizationId" }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
    }
}
