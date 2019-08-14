package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.response.TxHashResponse
import com.ampnet.crowdfundingbackend.enums.TransactionType
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.InternalException
import com.ampnet.crowdfundingbackend.exception.InvalidRequestException
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.persistence.model.TransactionInfo
import com.ampnet.crowdfundingbackend.service.DepositService
import com.ampnet.crowdfundingbackend.service.OrganizationService
import com.ampnet.crowdfundingbackend.service.ProjectInvestmentService
import com.ampnet.crowdfundingbackend.service.ProjectService
import com.ampnet.crowdfundingbackend.service.TransactionInfoService
import com.ampnet.crowdfundingbackend.service.WalletService
import com.ampnet.crowdfundingbackend.service.WithdrawService
import com.ampnet.crowdfundingbackend.websocket.WebSocketNotificationService
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class BroadcastTransactionController(
    private val transactionInfoService: TransactionInfoService,
    private val walletService: WalletService,
    private val organizationService: OrganizationService,
    private val projectService: ProjectService,
    private val projectInvestmentService: ProjectInvestmentService,
    private val depositService: DepositService,
    private val withdrawService: WithdrawService,
    private val notificationService: WebSocketNotificationService
) {

    companion object : KLogging()

    @PostMapping("/tx_broadcast")
    fun broadcastTransaction(
        @RequestParam(name = "tx_id", required = true) txId: Int,
        @RequestParam(name = "tx_sig", required = true) signedTransaction: String
    ): ResponseEntity<TxHashResponse> {
        logger.info { "Received request to broadcast transaction with id: $txId" }

        val transactionInfo = getTransactionInfo(txId)
        logger.info { "Broadcasting transaction: $transactionInfo" }

        val txHash = when (transactionInfo.type) {
            TransactionType.CREATE_ORG -> createOrganizationWallet(transactionInfo, signedTransaction)
            TransactionType.CREATE_PROJECT -> createProjectWallet(transactionInfo, signedTransaction)
            TransactionType.INVEST_ALLOWANCE -> projectInvestmentService.investInProject(signedTransaction)
            TransactionType.INVEST -> projectInvestmentService.confirmInvestment(signedTransaction)
            TransactionType.MINT -> confirmMintTransaction(transactionInfo, signedTransaction)
            TransactionType.BURN_APPROVAL -> confirmApprovalTransaction(transactionInfo, signedTransaction)
            TransactionType.BURN -> burnTransaction(transactionInfo, signedTransaction)
        }
        logger.info { "Successfully broadcast transaction. TxHash: $txHash" }

        notificationService.notifyTxBroadcast(txId, "BROADCAST")

        transactionInfoService.deleteTransaction(transactionInfo.id)
        return ResponseEntity.ok(TxHashResponse(txHash))
    }

    private fun createOrganizationWallet(transactionInfo: TransactionInfo, signedTransaction: String): String {
        val orgId = transactionInfo.companionId
                ?: throw InvalidRequestException(ErrorCode.TX_COMPANION_ID_MISSING, "Missing organization id")
        val organization = organizationService.findOrganizationById(orgId)
                ?: throw ResourceNotFoundException(ErrorCode.ORG_MISSING, "Missing organization with id: $orgId")
        val wallet = walletService.createOrganizationWallet(organization, signedTransaction)
        return wallet.hash
    }

    private fun createProjectWallet(transactionInfo: TransactionInfo, signedTransaction: String): String {
        val projectId = transactionInfo.companionId
                ?: throw InvalidRequestException(ErrorCode.TX_COMPANION_ID_MISSING, "Missing project id")
        val project = projectService.getProjectById(projectId)
                ?: throw ResourceNotFoundException(ErrorCode.PRJ_MISSING, "Missing project with id: $projectId")
        val wallet = walletService.createProjectWallet(project, signedTransaction)
        return wallet.hash
    }

    private fun confirmMintTransaction(transactionInfo: TransactionInfo, signedTransaction: String): String {
        val depositId = transactionInfo.companionId
                ?: throw InvalidRequestException(ErrorCode.TX_COMPANION_ID_MISSING, "Missing deposit id")
        val deposit = depositService.confirmMintTransaction(signedTransaction, depositId)
        return deposit.txHash
                ?: throw InternalException(ErrorCode.TX_MISSING, "Missing txHash for mint transaction")
    }

    private fun confirmApprovalTransaction(transactionInfo: TransactionInfo, signedTransaction: String): String {
        val withdrawId = getWithdrawIdFromTransactionInfo(transactionInfo)
        val withdraw = withdrawService.confirmApproval(signedTransaction, withdrawId)
        return withdraw.approvedTxHash
                ?: throw InternalException(ErrorCode.TX_MISSING, "Missing approvedTxHash for withdraw transaction")
    }

    private fun burnTransaction(transactionInfo: TransactionInfo, signedTransaction: String): String {
        val withdrawId = getWithdrawIdFromTransactionInfo(transactionInfo)
        val withdraw = withdrawService.burn(signedTransaction, withdrawId)
        return withdraw.burnedTxHash
                ?: throw InternalException(ErrorCode.TX_MISSING, "Missing burnedTxHash for withdraw transaction")
    }

    private fun getWithdrawIdFromTransactionInfo(transactionInfo: TransactionInfo): Int {
        return transactionInfo.companionId
                ?: throw InvalidRequestException(ErrorCode.TX_COMPANION_ID_MISSING, "Missing withdraw id")
    }

    private fun getTransactionInfo(txId: Int) = transactionInfoService.findTransactionInfo(txId)
            ?: throw ResourceNotFoundException(ErrorCode.TX_MISSING, "Non existing transaction with id: $txId")
}
