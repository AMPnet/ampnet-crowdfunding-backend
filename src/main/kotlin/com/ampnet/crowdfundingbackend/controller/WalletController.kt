package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.request.WalletCreateRequest
import com.ampnet.crowdfundingbackend.controller.pojo.response.WalletResponse
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.service.ProjectService
import com.ampnet.crowdfundingbackend.service.UserService
import com.ampnet.crowdfundingbackend.service.WalletService
import mu.KLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
class WalletController(
    private val walletService: WalletService,
    private val userService: UserService,
    private val projectService: ProjectService
) {

    companion object : KLogging()

    @GetMapping("/wallet")
    fun getMyWallet(): ResponseEntity<WalletResponse> {
        val user = ControllerUtils.getUserFromSecurityContext(userService)
        logger.debug("Received request for Wallet from user: ${user.email}")

        val wallet = user.wallet ?: return ResponseEntity.notFound().build()

        val balance = walletService.getWalletBalance(wallet)
        val response = WalletResponse(wallet, balance)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/wallet")
    fun createWallet(@RequestBody @Valid request: WalletCreateRequest): ResponseEntity<WalletResponse> {
        logger.debug { "Received request to create wallet: $request" }

        val user = ControllerUtils.getUserFromSecurityContext(userService)
        logger.debug("Received request to create a Wallet for user: ${user.email}")
        val wallet = walletService.createUserWallet(user, request.address)

        val balance = walletService.getWalletBalance(wallet)
        val response = WalletResponse(wallet, balance)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/wallet/project/{projectId}")
    fun getProjectWallet(@PathVariable projectId: Int): ResponseEntity<WalletResponse> {
        logger.debug { "Received request to get project($projectId) wallet" }

        val user = ControllerUtils.getUserFromSecurityContext(userService)
        logger.debug("Received request to create a Wallet project by user: ${user.email}")

        val project = projectService.getProjectByIdWithWallet(projectId)
                ?: throw ResourceNotFoundException(ErrorCode.PRJ_MISSING, "Missing project with id $projectId")

        if (project.createdBy.id == user.id) {
            project.wallet?.let {
                val balance = walletService.getWalletBalance(it)
                val response = WalletResponse(it, balance)
                return ResponseEntity.ok(response)
            }
            return ResponseEntity.notFound().build()
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
    }

    @PostMapping("/wallet/project/{projectId}")
    fun createProjectWallet(
        @PathVariable projectId: Int,
        @RequestBody @Valid request: WalletCreateRequest
    ): ResponseEntity<WalletResponse> {
        logger.debug { "Received request to create project($projectId) wallet: $request" }

        val user = ControllerUtils.getUserFromSecurityContext(userService)
        logger.debug("Received request to create a Wallet project by user: ${user.email}")

        val project = projectService.getProjectByIdWithWallet(projectId)
                ?: throw ResourceNotFoundException(ErrorCode.PRJ_MISSING, "Missing project with id $projectId")

        if (project.createdBy.id == user.id) {
            val wallet = walletService.createProjectWallet(project, request.address)
            val balance = walletService.getWalletBalance(wallet)
            val response = WalletResponse(wallet, balance)
            return ResponseEntity.ok(response)
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
    }
}
