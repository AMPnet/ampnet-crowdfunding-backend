package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.request.WalletCreateRequest
import com.ampnet.crowdfundingbackend.controller.pojo.response.PairWalletResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.TransactionResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.WalletResponse
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.service.OrganizationService
import com.ampnet.crowdfundingbackend.service.ProjectService
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

    /* User Wallet */
    @GetMapping("/wallet/pair/{code}")
    fun getPairWalletCode(@PathVariable code: String): ResponseEntity<PairWalletResponse> {
        logger.debug { "Received request getPairWalletCode" }
        walletService.getPairWalletCode(code)?.let {
            return ResponseEntity.ok(PairWalletResponse(it))
        }
        return ResponseEntity.notFound().build()
    }

    @PostMapping("/wallet/pair")
    fun generatePairWalletCode(@RequestBody @Valid request: WalletCreateRequest): ResponseEntity<PairWalletResponse> {
        logger.debug { "Received request to pair wallet: $request" }
        val pairWalletCode = walletService.generatePairWalletCode(request)
        return ResponseEntity.ok(PairWalletResponse(pairWalletCode))
    }

    @GetMapping("/wallet")
    fun getMyWallet(): ResponseEntity<WalletResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request for Wallet from user: ${userPrincipal.uuid}" }
        walletService.getUserWallet(userPrincipal.uuid)?.let {
            val balance = walletService.getWalletBalance(it)
            val response = WalletResponse(it, balance)
            return ResponseEntity.ok(response)
        }
        return ResponseEntity.notFound().build()
    }

    @PostMapping("/wallet")
    fun createWallet(@RequestBody @Valid request: WalletCreateRequest): ResponseEntity<WalletResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request from user: ${userPrincipal.uuid} to create wallet: $request" }
        val wallet = walletService.createUserWallet(userPrincipal.uuid, request)
        val response = WalletResponse(wallet, 0)
        return ResponseEntity.ok(response)
    }

    /* Project Wallet */
    @GetMapping("/wallet/project/{projectId}/transaction")
    fun getTransactionToCreateProjectWallet(@PathVariable projectId: Int): ResponseEntity<TransactionResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to create a Wallet for project: $projectId by user: ${userPrincipal.uuid}" }
        val project = projectService.getProjectByIdWithWallet(projectId)
                ?: throw ResourceNotFoundException(ErrorCode.PRJ_MISSING, "Missing project with id $projectId")

        if (project.createdByUserUuid == userPrincipal.uuid) {
            val transaction = walletService.generateTransactionToCreateProjectWallet(project, userPrincipal.uuid)
            val response = TransactionResponse(transaction)
            return ResponseEntity.ok(response)
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
    }

    /* Organization Wallet */
    @GetMapping("wallet/organization/{organizationId}")
    fun getOrganizationWallet(@PathVariable organizationId: Int): ResponseEntity<WalletResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug {
            "Received request to get Wallet for organization $organizationId by user: ${userPrincipal.email}"
        }
        val organization = organizationService.findOrganizationByIdWithWallet(organizationId)
                ?: throw ResourceNotFoundException(ErrorCode.ORG_MISSING, "Missing organization: $organizationId")

        organization.wallet?.let {
            val balance = walletService.getWalletBalance(it)
            return ResponseEntity.ok(WalletResponse(it, balance))
        }
        return ResponseEntity.notFound().build()
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

        if (organization.createdByUserUuid == userPrincipal.uuid) {
            val transaction = walletService
                    .generateTransactionToCreateOrganizationWallet(organization, userPrincipal.uuid)
            val response = TransactionResponse(transaction)
            return ResponseEntity.ok(response)
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
    }
}
