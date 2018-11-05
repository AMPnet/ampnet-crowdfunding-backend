package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.config.auth.UserPrincipal
import com.ampnet.crowdfundingbackend.controller.pojo.response.WalletResponse
import com.ampnet.crowdfundingbackend.service.UserService
import com.ampnet.crowdfundingbackend.service.WalletService
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

@RestController
class WalletController(val walletService: WalletService, val userService: UserService) {

    companion object : KLogging()

    @GetMapping("/wallet")
    fun getMyWallet(): ResponseEntity<WalletResponse> {
        val userPrincipal = SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        logger.debug("Received request for Wallet from user: ${userPrincipal.email}")
        // TODO: maybe include userId in user principal
        val user = userService.find(userPrincipal.email)
        val wallet = walletService.getWalletForUser(user!!.id)
                ?: return ResponseEntity.notFound().build() // or throw exception ResourceNotFound

        val balance = BigDecimal.ZERO
        val dateTime = ControllerUtils.zonedDateTimeToString(wallet.createdAt)
        val response = WalletResponse(wallet, balance, dateTime)
        return ResponseEntity.ok(response)
    }
}
