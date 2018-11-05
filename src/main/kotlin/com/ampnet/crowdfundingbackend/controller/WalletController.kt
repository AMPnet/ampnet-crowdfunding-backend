package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.config.auth.UserPrincipal
import com.ampnet.crowdfundingbackend.controller.pojo.response.WalletResponse
import com.ampnet.crowdfundingbackend.service.UserService
import com.ampnet.crowdfundingbackend.service.WalletService
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

@RestController
class WalletController(val walletService: WalletService, val userService: UserService) {

    companion object : KLogging()

    @GetMapping("/wallet")
    fun getMyWallet(): ResponseEntity<WalletResponse> {
        val userPrincipal = SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        logger.debug("Received request for Wallet from user: ${userPrincipal.email}")
        // TODO: maybe add user id to userPrincipal
        val userId = userService.find(userPrincipal.email)!!.id
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
        // TODO: maybe add user id to userPrincipal
        val userId = userService.find(userPrincipal.email)!!.id
        val wallet = walletService.createWallet(userId)

        val balance = BigDecimal.ZERO // balance should be zero after creating account
        val response = WalletResponse(wallet, balance)
        return ResponseEntity.ok(response)
    }
}
