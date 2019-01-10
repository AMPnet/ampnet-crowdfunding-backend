package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.config.auth.UserPrincipal
import com.ampnet.crowdfundingbackend.controller.pojo.request.SignedTransaction
import com.ampnet.crowdfundingbackend.controller.pojo.request.WalletCreateRequest
import com.ampnet.crowdfundingbackend.controller.pojo.response.TransactionResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.WalletResponse
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.service.ProjectService
import com.ampnet.crowdfundingbackend.service.UserService
import com.ampnet.crowdfundingbackend.service.WalletService
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
class WalletController(
    private val walletService: WalletService,
    private val userService: UserService,
    private val projectService: ProjectService
) {

    companion object : KLogging()

    @GetMapping("/wallet")
    fun getMyWallet(): ResponseEntity<WalletResponse> {
        val userPrincipal = SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        logger.debug("Received request for Wallet from user: ${userPrincipal.email}")
        val user = getUserWithWallet(userPrincipal.email)
        val wallet = user.wallet ?: return ResponseEntity.notFound().build()

        val balance = walletService.getWalletBalance(wallet)
        val response = WalletResponse(wallet, balance)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/wallet")
    fun createWallet(@RequestBody @Valid request: WalletCreateRequest): ResponseEntity<WalletResponse> {
        logger.debug { "Received request to create wallet: $request" }

        val userPrincipal = SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        logger.debug("Received request to create a Wallet for user: ${userPrincipal.email}")
        val user = getUserWithWallet(userPrincipal.email)
        val wallet = walletService.createUserWallet(user, request.address)

        val balance = walletService.getWalletBalance(wallet)
        val response = WalletResponse(wallet, balance)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/wallet/project/{projectId}")
    fun getProjectWallet(@PathVariable projectId: Int): ResponseEntity<WalletResponse> {
        logger.debug { "Received request to get project($projectId) wallet" }

        val userPrincipal = SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        logger.debug("Received request to create a Wallet project by user: ${userPrincipal.email}")

        val project = projectService.getProjectByIdWithWallet(projectId)
                ?: throw ResourceNotFoundException(ErrorCode.PRJ_MISSING, "Missing project with id $projectId")
        val user = getUser(userPrincipal.email)

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

    @GetMapping("/wallet/project/{projectId}/transaction")
    fun getTransactionToCreateProjectWallet(@PathVariable projectId: Int): ResponseEntity<TransactionResponse> {
        logger.debug { "Received request to create project($projectId) wallet" }

        val userPrincipal = SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        logger.debug("Received request to create a Wallet project by user: ${userPrincipal.email}")

        val project = projectService.getProjectByIdWithWallet(projectId)
                ?: throw ResourceNotFoundException(ErrorCode.PRJ_MISSING, "Missing project with id $projectId")
        val user = getUser(userPrincipal.email)

        if (project.createdBy.id == user.id) {
            val transaction = walletService.generateTransactionToCreateProjectWallet(project)
            val link = "/wallet/project/$projectId/transaction/signed"
            val response = TransactionResponse(transaction, link)
            return ResponseEntity.ok(response)
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
    }

    @PostMapping("/wallet/project/{projectId}/transaction/signed")
    fun createProjectWallet(
        @PathVariable projectId: Int,
        @RequestBody request: SignedTransaction
    ): ResponseEntity<WalletResponse> {
        logger.debug { "Received request to create project($projectId) wallet" }

        val project = projectService.getProjectByIdWithWallet(projectId)
                ?: throw ResourceNotFoundException(ErrorCode.PRJ_MISSING, "Missing project with id $projectId")
        val wallet = walletService.createProjectWallet(project, request.data)
        val response = WalletResponse(wallet, 0)
        return ResponseEntity.ok(response)
    }

    private fun getUserWithWallet(email: String): User {
        return userService.findWithWallet(email)
                ?: throw ResourceNotFoundException(ErrorCode.USER_MISSING, "Missing user with email: $email")
    }

    private fun getUser(email: String): User {
        return userService.find(email)
                ?: throw ResourceNotFoundException(ErrorCode.USER_MISSING, "Missing user with email: $email")
    }
}
