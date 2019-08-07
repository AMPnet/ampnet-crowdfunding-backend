package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.blockchain.BlockchainService
import com.ampnet.crowdfundingbackend.controller.pojo.request.SignedTransactionRequest
import com.ampnet.crowdfundingbackend.controller.pojo.response.TransactionAndLinkResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.TxHashResponse
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.InvalidRequestException
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.service.WalletService
import com.ampnet.crowdfundingbackend.service.pojo.PostTransactionType
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class IssuingAuthorityController(
    private val blockchainService: BlockchainService,
    private val walletService: WalletService
) {

    companion object : KLogging()

    private val transactionTypeLink = "/issuer/transaction/"

    @GetMapping("/issuer/mint")
    fun getMintTransaction(
        @RequestParam(name = "from") from: String,
        @RequestParam(name = "uuid") userUuid: UUID,
        @RequestParam(name = "amount") amount: Long
    ): ResponseEntity<TransactionAndLinkResponse> {
        logger.info { "Received mint request from=$from to user=$userUuid with amount=$amount" }
        val userWalletHash = getUserWalletHashFromUuid(userUuid)

        val transaction = blockchainService.generateMintTransaction(from, userWalletHash, amount)
        val link = transactionTypeLink + IssuerTransactionType.MINT.value
        logger.info { "Successfully generated mint transaction" }

        return ResponseEntity.ok(TransactionAndLinkResponse(transaction, link))
    }

    @GetMapping("/issuer/burn")
    fun getBurnTransaction(
        @RequestParam(name = "from") from: String,
        @RequestParam(name = "uuid") userUuid: UUID,
        @RequestParam(name = "amount") amount: Long
    ): ResponseEntity<TransactionAndLinkResponse> {
        logger.info { "Received burn request from=$from for user=$userUuid with amount=$amount" }
        val userWalletHash = getUserWalletHashFromUuid(userUuid)

        val transaction = blockchainService.generateBurnTransaction(from, userWalletHash, amount)
        val link = transactionTypeLink + IssuerTransactionType.BURN.value
        logger.info { "Successfully generated burn transaction" }

        return ResponseEntity.ok(TransactionAndLinkResponse(transaction, link))
    }

    @PostMapping("/issuer/transaction/{type}")
    fun postTransaction(
        @RequestBody request: SignedTransactionRequest,
        @PathVariable(value = "type") type: String
    ): ResponseEntity<TxHashResponse> {
        logger.info { "Received request to post issuer transaction, type = $type" }
        val postTransactionType = when (type) {
            IssuerTransactionType.BURN.value -> PostTransactionType.ISSUER_BURN
            IssuerTransactionType.MINT.value -> PostTransactionType.ISSUER_MINT
            else -> throw InvalidRequestException(ErrorCode.INT_INVALID_VALUE, "Invalid transaction type value")
        }
        val txHash = blockchainService.postTransaction(request.data, postTransactionType)
        logger.info { "Issuer successfully posted transaction, type = $type. TxHash = $txHash" }

        return ResponseEntity.ok(TxHashResponse(txHash))
    }

    private fun getUserWalletHashFromUuid(userUuid: UUID) =
            walletService.getUserWallet(userUuid)?.hash
                    ?: throw ResourceNotFoundException(ErrorCode.WALLET_MISSING, "User does not have a wallet")

    enum class IssuerTransactionType(val value: String) {
        MINT("mint"), BURN("burn")
    }
}
