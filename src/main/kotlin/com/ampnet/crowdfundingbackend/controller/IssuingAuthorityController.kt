package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.blockchain.BlockchainService
import com.ampnet.crowdfundingbackend.controller.pojo.request.SignedTransactionRequest
import com.ampnet.crowdfundingbackend.controller.pojo.response.TransactionResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.TxHashResponse
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.service.UserService
import com.ampnet.crowdfundingbackend.service.pojo.PostTransactionType
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class IssuingAuthorityController(
    private val blockchainService: BlockchainService,
    private val userService: UserService
) {

    companion object : KLogging()

    private val transactionTypeLink = "/issuer/transaction?type="

    @GetMapping("/issuer/mint")
    fun getMintTransaction(
        @RequestParam(name = "from") from: String,
        @RequestParam(name = "email") email: String,
        @RequestParam(name = "amount") amount: Long
    ): ResponseEntity<TransactionResponse> {
        logger.info { "Received mint request from=$from to email=$email in amount=$amount" }
        val userWalletHash = getUserWalletHashFromEmail(email)

        val transaction = blockchainService.generateMintTransaction(from, userWalletHash, amount)
        val link = "$transactionTypeLink${IssuerTransactionType.mint}"
        logger.info { "Successfully generated mint transaction" }

        return ResponseEntity.ok(TransactionResponse(transaction, link))
    }

    @GetMapping("/issuer/burn")
    fun getBurnTransaction(
        @RequestParam(name = "from") from: String,
        @RequestParam(name = "email") email: String,
        @RequestParam(name = "amount") amount: Long
    ): ResponseEntity<TransactionResponse> {
        logger.info { "Received burn request from=$from for email=$email in amount=$amount" }
        val userWalletHash = getUserWalletHashFromEmail(email)

        val transaction = blockchainService.generateBurnTransaction(from, userWalletHash, amount)
        val link = "$transactionTypeLink${IssuerTransactionType.burn}"
        logger.info { "Successfully generated burn transaction" }

        return ResponseEntity.ok(TransactionResponse(transaction, link))
    }

    @PostMapping("/issuer/transaction")
    fun postTransaction(
        @RequestBody request: SignedTransactionRequest,
        @RequestParam(name = "type") type: IssuerTransactionType
    ): ResponseEntity<TxHashResponse> {
        logger.info { "Received request to post issuer transaction, type = $type" }
        val postTransactionType = when (type) {
            IssuerTransactionType.burn -> PostTransactionType.ISSUER_BURN
            IssuerTransactionType.mint -> PostTransactionType.ISSUER_MINT
        }
        val txHash = blockchainService.postTransaction(request.data, postTransactionType)
        logger.info { "Issuer successfully posted transaction, type = $type. TxHash = $txHash" }

        return ResponseEntity.ok(TxHashResponse(txHash))
    }

    private fun getUserWalletHashFromEmail(email: String): String {
        val user = userService.findWithWallet(email)
            ?: throw ResourceNotFoundException(ErrorCode.USER_MISSING, "Missing user with email: $email")
        return user.wallet?.hash
            ?: throw ResourceNotFoundException(ErrorCode.WALLET_MISSING, "User does not have a wallet")
    }

    enum class IssuerTransactionType {
        mint, burn
    }
}
