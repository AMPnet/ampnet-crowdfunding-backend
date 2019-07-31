package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.request.WithdrawApproveRequest
import com.ampnet.crowdfundingbackend.controller.pojo.request.WithdrawCreateRequest
import com.ampnet.crowdfundingbackend.controller.pojo.response.WithdrawResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.WithdrawWithUserAndAcceptanceListResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.WithdrawWithUserAndAcceptanceResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.WithdrawWithUserListResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.WithdrawWithUserResponse
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

    @GetMapping("/api/v1/withdraw/unapproved")
    @PreAuthorize("hasAuthority(T(com.ampnet.crowdfundingbackend.enums.PrivilegeType).PRA_WITHDRAW)")
    fun getUnapprovedWithdraws(): ResponseEntity<WithdrawWithUserListResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to get Withdraws by user: ${userPrincipal.uuid}" }
        val withdraws = withdrawService.getWithdraws(false)
        val users = userService.getUsers(withdraws.map { it.userUuid })
        val withdrawWithUserList = mutableListOf<WithdrawWithUserResponse>()
        withdraws.forEach { withdraw ->
            val wallet = walletService.getUserWallet(withdraw.userUuid)?.hash.orEmpty()
            val userUuid = withdraw.userUuid.toString()
            val userResponse = users.find { it.uuid == userUuid }
            withdrawWithUserList.add(WithdrawWithUserResponse(withdraw, userResponse, wallet))
        }
        return ResponseEntity.ok(WithdrawWithUserListResponse(withdrawWithUserList))
    }

    @GetMapping("/api/v1/withdraw/approved")
    @PreAuthorize("hasAuthority(T(com.ampnet.crowdfundingbackend.enums.PrivilegeType).PRA_WITHDRAW)")
    fun getApprovedWithdraws(): ResponseEntity<WithdrawWithUserAndAcceptanceListResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to get Withdraws by user: ${userPrincipal.uuid}" }
        val withdraws = withdrawService.getWithdraws(true)
        val users = userService.getUsers(withdraws.map { it.userUuid })
        val acceptors = userService.getUsers(withdraws.mapNotNull { it.approvedByUserUuid })
        val withdrawWithUserList = mutableListOf<WithdrawWithUserAndAcceptanceResponse>()
        withdraws.forEach { withdraw ->
            val wallet = walletService.getUserWallet(withdraw.userUuid)?.hash.orEmpty()
            val userUuid = withdraw.userUuid.toString()
            val acceptorUuid = withdraw.approvedByUserUuid.toString()
            val userResponse = users.find { it.uuid == userUuid }
            val acceptor = acceptors.find { it.uuid == acceptorUuid }
            withdrawWithUserList.add(WithdrawWithUserAndAcceptanceResponse(withdraw, userResponse, acceptor, wallet))
        }
        return ResponseEntity.ok(WithdrawWithUserAndAcceptanceListResponse(withdrawWithUserList))
    }

    @PostMapping("/api/v1/withdraw/{id}/approve")
    @PreAuthorize("hasAuthority(T(com.ampnet.crowdfundingbackend.enums.PrivilegeType).PWA_WITHDRAW)")
    fun approveWithdraw(
        @RequestBody request: WithdrawApproveRequest,
        @PathVariable("id") id: Int
    ): ResponseEntity<WithdrawResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.info { "Received request to approve Withdraw: $id by user: ${userPrincipal.uuid}" }
        val withdraw = withdrawService.approveWithdraw(userPrincipal.uuid, id, request.reference)
        return ResponseEntity.ok(WithdrawResponse(withdraw))
    }
}
