package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.config.auth.UserPrincipal
import com.ampnet.crowdfundingbackend.controller.pojo.request.WalletCreateRequest
import com.ampnet.crowdfundingbackend.controller.pojo.response.WalletResponse
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.service.UserService
import com.ampnet.crowdfundingbackend.service.WalletService
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
class WalletController(val walletService: WalletService, val userService: UserService) {

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

    private fun getUserWithWallet(email: String): User {
        // think about adding UserId to UserPrincipal
        return userService.findWithWallet(email)
                ?: throw ResourceNotFoundException(ErrorCode.USER_MISSING, "Missing user with email: $email")
    }
}
