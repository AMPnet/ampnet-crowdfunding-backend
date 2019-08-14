package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.blockchain.BlockchainService
import com.ampnet.crowdfundingbackend.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.InvalidRequestException
import com.ampnet.crowdfundingbackend.exception.ResourceAlreadyExistsException
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.persistence.model.Withdraw
import com.ampnet.crowdfundingbackend.persistence.repository.UserWalletRepository
import com.ampnet.crowdfundingbackend.persistence.repository.WithdrawRepository
import com.ampnet.crowdfundingbackend.service.MailService
import com.ampnet.crowdfundingbackend.service.StorageService
import com.ampnet.crowdfundingbackend.service.TransactionInfoService
import com.ampnet.crowdfundingbackend.service.WithdrawService
import com.ampnet.crowdfundingbackend.service.pojo.DocumentSaveRequest
import com.ampnet.crowdfundingbackend.service.pojo.PostTransactionType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

@Service
class WithdrawServiceImpl(
    private val withdrawRepository: WithdrawRepository,
    private val userWalletRepository: UserWalletRepository,
    private val blockchainService: BlockchainService,
    private val transactionInfoService: TransactionInfoService,
    private val storageService: StorageService,
    private val mailService: MailService
) : WithdrawService {

    // TODO: remove after changing blockchain-service
    private val burnWallet = "0x43b0d9b605e68a0c50dc436757a86c82d97787cc"

    @Transactional(readOnly = true)
    override fun getPendingForUser(user: UUID): Withdraw? {
        return withdrawRepository.findByUserUuid(user).find { it.approvedTxHash == null }
    }

    @Transactional(readOnly = true)
    override fun getAllApproved(): List<Withdraw> {
        return withdrawRepository.findAllApproved()
    }

    @Transactional(readOnly = true)
    override fun getAllBurned(): List<Withdraw> {
        return withdrawRepository.findAllBurned()
    }

    @Transactional
    override fun createWithdraw(user: UUID, amount: Long, bankAccount: String): Withdraw {
        validateUserDoesNotHavePendingWithdraw(user)
        checkIfUserHasEnoughFunds(user, amount)
        val withdraw = Withdraw(0, user, amount, ZonedDateTime.now(), bankAccount,
                null, null, null, null, null, null)
        withdrawRepository.save(withdraw)
        mailService.sendWithdrawRequest(user, amount)
        return withdraw
    }

    @Transactional
    override fun deleteWithdraw(withdrawId: Int) {
        val withdraw = getWithdraw(withdrawId)
        if (withdraw.burnedTxHash != null) {
            throw InvalidRequestException(ErrorCode.WALLET_WITHDRAW_BURNED, "Cannot delete burned Withdraw")
        }
        withdrawRepository.delete(withdraw)
        mailService.sendWithdrawInfo(withdraw.userUuid, false)
    }

    @Transactional
    override fun generateApprovalTransaction(withdrawId: Int, user: UUID): TransactionDataAndInfo {
        val withdraw = getWithdraw(withdrawId)
        if (withdraw.userUuid != user) {
            throw InvalidRequestException(ErrorCode.WALLET_WITHDRAW_MISSING, "Withdraw does not belong to this user")
        }
        validateWithdrawForApproval(withdraw)
        val userWallet = getUserWallet(withdraw.userUuid)
        val data = blockchainService.generateApproveBurnTransaction(userWallet, withdraw.amount)
        val info = transactionInfoService.createApprovalTransaction(withdraw.amount, user, withdraw.id)
        return TransactionDataAndInfo(data, info)
    }

    @Transactional
    override fun confirmApproval(signedTransaction: String, withdrawId: Int): Withdraw {
        val withdraw = getWithdraw(withdrawId)
        validateWithdrawForApproval(withdraw)
        val approvalTxHash = blockchainService.postTransaction(signedTransaction, PostTransactionType.APPROVAL_BURN)
        withdraw.approvedTxHash = approvalTxHash
        withdraw.approvedAt = ZonedDateTime.now()
        return withdrawRepository.save(withdraw)
    }

    @Transactional
    override fun generateBurnTransaction(withdrawId: Int, user: UUID): TransactionDataAndInfo {
        val withdraw = getWithdraw(withdrawId)
        validateWithdrawForBurn(withdraw)
        val userWallet = getUserWallet(withdraw.userUuid)
        val data = blockchainService.generateBurnTransaction(burnWallet, userWallet, withdraw.amount)
        val info = transactionInfoService.createBurnTransaction(withdraw.amount, user, withdraw.id)
        withdraw.burnedBy = user
        withdrawRepository.save(withdraw)
        return TransactionDataAndInfo(data, info)
    }

    @Transactional
    override fun burn(signedTransaction: String, withdrawId: Int): Withdraw {
        val withdraw = getWithdraw(withdrawId)
        validateWithdrawForBurn(withdraw)
        val burnedTxHash = blockchainService.postTransaction(signedTransaction, PostTransactionType.ISSUER_BURN)
        withdraw.burnedTxHash = burnedTxHash
        withdraw.burnedAt = ZonedDateTime.now()
        withdrawRepository.save(withdraw)
        mailService.sendWithdrawInfo(withdraw.userUuid, true)
        return withdraw
    }

    @Transactional
    override fun addDocument(withdrawId: Int, request: DocumentSaveRequest): Withdraw {
        val withdraw = getWithdraw(withdrawId)
        val document = storageService.saveDocument(request)
        withdraw.document = document
        return withdrawRepository.save(withdraw)
    }

    private fun checkIfUserHasEnoughFunds(user: UUID, amount: Long) {
        val wallet = userWalletRepository.findByUserUuid(user).orElseThrow {
            throw ResourceNotFoundException(ErrorCode.WALLET_MISSING,
                    "User must have a wallet to make Withdraw request")
        }
        val balance = blockchainService.getBalance(wallet.wallet.hash)
        if (amount > balance) {
            throw InvalidRequestException(ErrorCode.WALLET_FUNDS, "Insufficient funds")
        }
    }

    private fun validateUserDoesNotHavePendingWithdraw(user: UUID) {
        withdrawRepository.findByUserUuid(user).forEach {
            if (it.approvedTxHash == null) {
                throw ResourceAlreadyExistsException(ErrorCode.WALLET_WITHDRAW_EXISTS, "Unapproved Withdraw: ${it.id}")
            }
            if (it.approvedTxHash != null && it.burnedTxHash == null) {
                throw ResourceAlreadyExistsException(ErrorCode.WALLET_WITHDRAW_EXISTS, "Unburned Withdraw: ${it.id}")
            }
        }
    }

    private fun validateWithdrawForApproval(withdraw: Withdraw) {
        if (withdraw.approvedTxHash != null) {
            throw InvalidRequestException(
                    ErrorCode.WALLET_WITHDRAW_APPROVED, "Approved txHash: ${withdraw.approvedTxHash}")
        }
    }

    private fun validateWithdrawForBurn(withdraw: Withdraw) {
        if (withdraw.approvedTxHash == null) {
            throw InvalidRequestException(
                    ErrorCode.WALLET_WITHDRAW_NOT_APPROVED, "Withdraw must be approved")
        }
        if (withdraw.burnedTxHash != null) {
            throw InvalidRequestException(ErrorCode.WALLET_WITHDRAW_BURNED, "Burned txHash: ${withdraw.burnedTxHash}")
        }
    }

    private fun getWithdraw(withdrawId: Int): Withdraw {
        return withdrawRepository.findById(withdrawId).orElseThrow {
            throw ResourceNotFoundException(ErrorCode.WALLET_WITHDRAW_MISSING, "Missing withdraw with id: $withdrawId")
        }
    }

    private fun getUserWallet(user: UUID): String {
        val userWallet = userWalletRepository.findByUserUuid(user).orElseThrow {
            throw ResourceNotFoundException(ErrorCode.WALLET_MISSING,
                    "User must have a wallet to create Withdraw request")
        }
        return userWallet.wallet.hash
    }
}
