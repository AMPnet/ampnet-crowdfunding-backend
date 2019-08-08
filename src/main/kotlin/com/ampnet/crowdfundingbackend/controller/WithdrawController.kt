package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.request.WithdrawCreateRequest
import com.ampnet.crowdfundingbackend.controller.pojo.response.TransactionResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.WithdrawResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.WithdrawWithUserListResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.WithdrawWithUserResponse
import com.ampnet.crowdfundingbackend.persistence.model.Withdraw
import com.ampnet.crowdfundingbackend.service.WalletService
import com.ampnet.crowdfundingbackend.service.WithdrawService
import com.ampnet.crowdfundingbackend.userservice.UserService
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class WithdrawController(
    private val withdrawService: WithdrawService,
    private val userService: UserService,
    private val walletService: WalletService
) {

    companion object : KLogging()

    @PostMapping("/api/v1/withdraw")
    fun createWithdraw(@RequestBody request: WithdrawCreateRequest): ResponseEntity<WithdrawResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to create Withdraw:$request by user: ${userPrincipal.uuid}" }
        val withdraw = withdrawService.createWithdraw(userPrincipal.uuid, request.amount)
        return ResponseEntity.ok(WithdrawResponse(withdraw))
    }

    @GetMapping("/api/v1/withdraw/approved")
    @PreAuthorize("hasAuthority(T(com.ampnet.crowdfundingbackend.enums.PrivilegeType).PRA_WITHDRAW)")
    fun getApprovedWithdraws(): ResponseEntity<WithdrawWithUserListResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to get all approved withdraws by user: ${userPrincipal.uuid}" }
        val response = generateResponseFromWithdraws( withdrawService.getAllApproved())
        return ResponseEntity.ok(response)
    }

    @GetMapping("/api/v1/withdraw/burned")
    @PreAuthorize("hasAuthority(T(com.ampnet.crowdfundingbackend.enums.PrivilegeType).PRA_WITHDRAW)")
    fun getBurnedWithdraws(): ResponseEntity<WithdrawWithUserListResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to get all burned withdraws by user: ${userPrincipal.uuid}" }
        val response = generateResponseFromWithdraws(withdrawService.getAllBurned())
        return ResponseEntity.ok(response)
    }

    @PostMapping("/api/v1/withdraw/{id}/transaction/approve")
    fun generateApproveTransaction(@PathVariable("id") id: Int): ResponseEntity<TransactionResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.info { "Received request to generate withdraw approval transaction by user: ${userPrincipal.uuid}" }
        val transactionDataAndInfo = withdrawService.generateApprovalTransaction(id, userPrincipal.uuid)
        return ResponseEntity.ok(TransactionResponse(transactionDataAndInfo))
    }

    @GetMapping("/api/v1/withdraw/{id}/transaction/burn")
    @PreAuthorize("hasAuthority(T(com.ampnet.crowdfundingbackend.enums.PrivilegeType).PWA_WITHDRAW)")
    fun generateBurnTransaction(@PathVariable("id") id: Int): ResponseEntity<TransactionResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.info { "Received request to generate withdraw burn transaction by user: ${userPrincipal.uuid}" }
        val transactionDataAndInfo = withdrawService.generateBurnTransaction(id, userPrincipal.uuid)
        return ResponseEntity.ok(TransactionResponse(transactionDataAndInfo))
    }

    private fun generateResponseFromWithdraws(withdraws: List<Withdraw>): WithdrawWithUserListResponse {
        val users = userService.getUsers(withdraws.map { it.userUuid })
        val withdrawWithUserList = mutableListOf<WithdrawWithUserResponse>()
        withdraws.forEach { withdraw ->
            val wallet = walletService.getUserWallet(withdraw.userUuid)?.hash.orEmpty()
            val userUuid = withdraw.userUuid.toString()
            val userResponse = users.find { it.uuid == userUuid }
            withdrawWithUserList.add(WithdrawWithUserResponse(withdraw, userResponse, wallet))
        }
        return WithdrawWithUserListResponse(withdrawWithUserList)
    }
}
