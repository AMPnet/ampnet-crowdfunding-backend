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
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
class WalletController(
    private val walletService: WalletService,
    private val projectService: ProjectService,
    private val organizationService: OrganizationService
) {

    companion object : KLogging()

    @GetMapping("/wallet")
    fun getMyWallet(): ResponseEntity<WalletResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request for Wallet from user: ${userPrincipal.uuid}" }
        val wallet = walletService.getUserWallet(userPrincipal.uuid)
                ?: return ResponseEntity.notFound().build()

        val balance = walletService.getWalletBalance(wallet)
        val response = WalletResponse(wallet, balance)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/wallet")
    fun createWallet(@RequestBody @Valid request: WalletCreateRequest): ResponseEntity<WalletResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request from user: ${userPrincipal.uuid} to create wallet: $request" }
        val wallet = walletService.createUserWallet(userPrincipal.uuid, request)
        val response = WalletResponse(wallet, 0)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/wallet/project/{projectId}")
    fun getProjectWallet(@PathVariable projectId: Int): ResponseEntity<WalletResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug {
            "Received request to get Wallet for project: $projectId wallet by user: ${userPrincipal.uuid}"
        }
        val project = projectService.getProjectByIdWithWallet(projectId)
                ?: throw ResourceNotFoundException(ErrorCode.PRJ_MISSING, "Missing project with id $projectId")

        // TODO: rethink about who can get Project wallet
//        if (project.createdBy.id == user.id) {
            project.wallet?.let {
                val balance = walletService.getWalletBalance(it)
                val response = WalletResponse(it, balance)
                return ResponseEntity.ok(response)
            }
            return ResponseEntity.notFound().build()
//        }
//        return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
    }

    @GetMapping("/wallet/project/{projectId}/transaction")
    fun getTransactionToCreateProjectWallet(@PathVariable projectId: Int): ResponseEntity<TransactionResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug{ "Received request to create a Wallet for project: $projectId by user: ${userPrincipal.uuid}" }
        val project = projectService.getProjectByIdWithWallet(projectId)
                ?: throw ResourceNotFoundException(ErrorCode.PRJ_MISSING, "Missing project with id $projectId")

//        if (project.createdBy.id == user.id) {
            val transaction = walletService.generateTransactionToCreateProjectWallet(project, userPrincipal.uuid)
            val response = TransactionResponse(transaction)
            return ResponseEntity.ok(response)
//        }
//        return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
    }

    @GetMapping("wallet/organization/{organizationId}")
    fun getOrganizationWallet(@PathVariable organizationId: Int): ResponseEntity<WalletResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug {
            "Received request to get Wallet for organization $organizationId by user: ${userPrincipal.email}"
        }
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
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug {
            "Received request to create Wallet for Organization: $organizationId by user: ${userPrincipal.email}"
        }
        val organization = organizationService.findOrganizationByIdWithWallet(organizationId)
                ?: throw ResourceNotFoundException(ErrorCode.ORG_MISSING, "Missing organization: $organizationId")

        // TODO: rethink about define who can create organization wallet
        if (organization.createdByUserUuid == userPrincipal.uuid) {
            val transaction = walletService
                    .generateTransactionToCreateOrganizationWallet(organization, userPrincipal.uuid)
            val response = TransactionResponse(transaction)
            return ResponseEntity.ok(response)
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
    }
}
