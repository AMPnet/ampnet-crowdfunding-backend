package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.request.WalletCreateRequest
import com.ampnet.crowdfundingbackend.controller.pojo.response.TransactionResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.WalletResponse
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.service.OrganizationService
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
class WalletController(
    private val walletService: WalletService,
    private val userService: UserService,
    private val projectService: ProjectService,
    private val organizationService: OrganizationService
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
        val user = ControllerUtils.getUserFromSecurityContext(userService)
        logger.debug { "Received request from user: ${user.email} to create wallet: $request" }
        val wallet = walletService.createUserWallet(user, request)
        val response = WalletResponse(wallet, 0)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/wallet/project/{projectId}")
    fun getProjectWallet(@PathVariable projectId: Int): ResponseEntity<WalletResponse> {
        logger.debug { "Received request to get project($projectId) wallet" }

        val user = ControllerUtils.getUserFromSecurityContext(userService)
        logger.debug("Received request to create a Wallet project by user: ${user.email}")

        val project = projectService.getProjectByIdWithWallet(projectId)
                ?: throw ResourceNotFoundException(ErrorCode.PRJ_MISSING, "Missing project with id $projectId")

        // TODO: rethink about who can get Project wallet
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

        val user = ControllerUtils.getUserFromSecurityContext(userService)
        logger.debug("Received request to create a Wallet project by user: ${user.email}")

        val project = projectService.getProjectByIdWithWallet(projectId)
                ?: throw ResourceNotFoundException(ErrorCode.PRJ_MISSING, "Missing project with id $projectId")

        if (project.createdBy.id == user.id) {
            val transaction = walletService.generateTransactionToCreateProjectWallet(project, user.id)
            val link = ControllerUtils.appendLinkWithTransactionRequestParam("/wallet/project/$projectId/transaction")
            val response = TransactionResponse(transaction, link)
            return ResponseEntity.ok(response)
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
    }

    @PostMapping("/wallet/project/{projectId}/transaction")
    fun createProjectWallet(
        @PathVariable projectId: Int,
        @RequestParam("d") signedTransaction: String
    ): ResponseEntity<WalletResponse> {
        logger.debug { "Received request to create project($projectId) wallet" }

        val project = projectService.getProjectByIdWithWallet(projectId)
                ?: throw ResourceNotFoundException(ErrorCode.PRJ_MISSING, "Missing project with id $projectId")
        val wallet = walletService.createProjectWallet(project, signedTransaction)
        val response = WalletResponse(wallet, 0)
        return ResponseEntity.ok(response)
    }

    @GetMapping("wallet/organization/{organizationId}")
    fun getOrganizationWallet(@PathVariable organizationId: Int): ResponseEntity<WalletResponse> {
        logger.debug { "Received request to get organization wallet: $organizationId" }

        val userPrincipal = ControllerUtils.getUserFromSecurityContext(userService)
        logger.debug("Received request to create a Wallet project by user: ${userPrincipal.email}")
        val organization = organizationService.findOrganizationByIdWithWallet(organizationId)
                ?: throw ResourceNotFoundException(ErrorCode.ORG_MISSING, "Missing organization: $organizationId")

        // TODO: rethink about who can get Organization wallet
        val wallet = organization.wallet
                ?: throw ResourceNotFoundException(ErrorCode.WALLET_MISSING,
                        "Missing wallet for organization: $organizationId")
        val balance = walletService.getWalletBalance(wallet)

        return ResponseEntity.ok(WalletResponse(wallet, balance))
    }

    @GetMapping("wallet/organization/{organizationId}/transaction")
    fun getTransactionToCreateOrganizationWallet(
        @PathVariable organizationId: Int
    ): ResponseEntity<TransactionResponse> {
        logger.debug { "Received request to create organization wallet: $organizationId" }

        val user = ControllerUtils.getUserFromSecurityContext(userService)
        logger.debug("Received request to create a Organization wallet by user: ${user.email}")

        val organization = organizationService.findOrganizationByIdWithWallet(organizationId)
                ?: throw ResourceNotFoundException(ErrorCode.ORG_MISSING, "Missing organization: $organizationId")

        // TODO: rethink about define who can create organization wallet
        if (organization.createdByUser.id == user.id) {
            val transaction = walletService.generateTransactionToCreateOrganizationWallet(organization, user.id)
            val link = ControllerUtils
                .appendLinkWithTransactionRequestParam("/wallet/organization/$organizationId/transaction")
            val response = TransactionResponse(transaction, link)
            return ResponseEntity.ok(response)
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
    }

    @PostMapping("/wallet/organization/{organizationId}/transaction")
    fun createOrganizationWallet(
        @PathVariable organizationId: Int,
        @RequestParam("d") signedTransaction: String
    ): ResponseEntity<WalletResponse> {
        logger.debug { "Received request to create organization($organizationId) wallet" }

        val organization = organizationService.findOrganizationByIdWithWallet(organizationId)
                ?: throw ResourceNotFoundException(
                    ErrorCode.ORG_MISSING, "Missing organization with id $organizationId")
        val wallet = walletService.createOrganizationWallet(organization, signedTransaction)
        val response = WalletResponse(wallet, 0)
        return ResponseEntity.ok(response)
    }
}
