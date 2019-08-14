package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.response.ProjectListResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.ProjectResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.ProjectWithFundingResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.WalletResponse
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.service.ProjectService
import com.ampnet.crowdfundingbackend.service.WalletService
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class ProjectPublicController(private val projectService: ProjectService, private val walletService: WalletService) {

    companion object : KLogging()

    @GetMapping("/public/project/{id}")
    fun getProject(@PathVariable id: Int): ResponseEntity<ProjectWithFundingResponse> {
        logger.debug { "Received request to get project with id: $id" }
        projectService.getProjectByIdWithAllData(id)?.let { project ->
            val currentFunding = project.wallet?.let { it -> walletService.getWalletBalance(it) }
            logger.debug { "Project $id current funding is: $currentFunding" }
            val response = ProjectWithFundingResponse(project, currentFunding)
            return ResponseEntity.ok(response)
        }
        return ResponseEntity.notFound().build()
    }

    @GetMapping("/public/project")
    fun getAllActiveProjectsWithWallet(): ResponseEntity<ProjectListResponse> {
        logger.debug { "Received request to get project all projects" }
        val projectsResponse = projectService.getAllActiveWithWallet().map { ProjectResponse(it) }
        val response = ProjectListResponse(projectsResponse)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/public/wallet/project/{projectId}")
    fun getProjectWallet(@PathVariable projectId: Int): ResponseEntity<WalletResponse> {
        logger.debug { "Received request to get wallet for project: $projectId" }
        val project = projectService.getProjectByIdWithWallet(projectId)
                ?: throw ResourceNotFoundException(ErrorCode.PRJ_MISSING, "Missing project with id $projectId")

        project.wallet?.let {
            val balance = walletService.getWalletBalance(it)
            val response = WalletResponse(it, balance)
            return ResponseEntity.ok(response)
        }
        return ResponseEntity.notFound().build()
    }
}
