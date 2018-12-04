package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.config.auth.UserPrincipal
import com.ampnet.crowdfundingbackend.controller.pojo.request.WalletDepositRequest
import com.ampnet.crowdfundingbackend.controller.pojo.response.TransactionResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.WalletResponse
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.enums.Currency
import com.ampnet.crowdfundingbackend.service.UserService
import com.ampnet.crowdfundingbackend.service.WalletService
import com.ampnet.crowdfundingbackend.service.pojo.DepositRequest
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

@RestController
class WalletController(val walletService: WalletService, val userService: UserService) {

    companion object : KLogging()

    @GetMapping("/wallet")
    fun getMyWallet(): ResponseEntity<WalletResponse> {
        val userPrincipal = SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        logger.debug("Received request for Wallet from user: ${userPrincipal.email}")
        val userId = getUserIdFromEmail(userPrincipal.email)
        val wallet = walletService.getWalletWithTransactionsForUser(userId)
                ?: return ResponseEntity.notFound().build() // or throw exception ResourceNotFound

        val balance = walletService.getWalletBalance(wallet)
        val response = WalletResponse(wallet, balance)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/wallet/create")
    fun createWallet(): ResponseEntity<WalletResponse> {
        val userPrincipal = SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        logger.debug("Received request to create a Wallet for user: ${userPrincipal.email}")
        val userId = getUserIdFromEmail(userPrincipal.email)
        val wallet = walletService.createWallet(userId)

        val balance = BigDecimal.ZERO // balance should be zero after creating account, or define other flow
        val response = WalletResponse(wallet, balance)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/wallet/deposit")
    fun depositToWallet(@RequestBody request: WalletDepositRequest): ResponseEntity<TransactionResponse> {
        val userPrincipal = SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        logger.debug("Received request to deposit to Wallet for user: ${userPrincipal.email}")
        val userId = getUserIdFromEmail(userPrincipal.email)
        val wallet = walletService.getWalletWithTransactionsForUser(userId)
                ?: throw ResourceNotFoundException("Missing wallet for user: ${userPrincipal.email}")

        // TODO: define the process with blockchain, getting hash, async actions
        val hash = "hash"
        val depositRequest = DepositRequest(wallet, request.amount, Currency.EUR, request.sender, hash)
        logger.debug("Creating deposit: $depositRequest")
        val transaction = walletService.depositToWallet(depositRequest)
        logger.debug("Successful deposit with transaction: $transaction")
        return ResponseEntity.ok(TransactionResponse(transaction))
    }

    private fun getUserIdFromEmail(email: String): Int {
        // think about adding UserId to UserPrincipal
        return userService.find(email)?.id
                ?: throw ResourceNotFoundException("Missing user with email: $email")
    }
}
