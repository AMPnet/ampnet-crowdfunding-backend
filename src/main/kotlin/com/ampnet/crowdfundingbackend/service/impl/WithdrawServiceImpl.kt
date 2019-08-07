package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.blockchain.BlockchainService
import com.ampnet.crowdfundingbackend.blockchain.pojo.TransactionData
import com.ampnet.crowdfundingbackend.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.InvalidRequestException
import com.ampnet.crowdfundingbackend.exception.ResourceAlreadyExistsException
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.persistence.model.Withdraw
import com.ampnet.crowdfundingbackend.persistence.repository.UserWalletRepository
import com.ampnet.crowdfundingbackend.persistence.repository.WithdrawRepository
import com.ampnet.crowdfundingbackend.service.TransactionInfoService
import com.ampnet.crowdfundingbackend.service.WithdrawService
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
    private val transactionInfoService: TransactionInfoService
) : WithdrawService {

    @Transactional(readOnly = true)
    override fun getWithdraws(approved: Boolean): List<Withdraw> {
        // TODO: change
        return withdrawRepository.findAll()
    }

    @Transactional
    override fun createWithdraw(user: UUID, amount: Long): Withdraw {
        if (userWalletRepository.findByUserUuid(user).isPresent.not()) {
            throw ResourceNotFoundException(ErrorCode.WALLET_MISSING,
                    "User must have a wallet to make Withdraw request")
        }
        withdrawRepository.findByUser(user).forEach {
            if (it.approvedTxHash == null) {
                throw ResourceAlreadyExistsException(ErrorCode.WALLET_WITHDRAW_EXISTS, "Unapproved Withdraw: ${it.id}")
            }
            if (it.approvedTxHash != null && it.burnedTxHash == null) {
                throw ResourceAlreadyExistsException(ErrorCode.WALLET_WITHDRAW_EXISTS, "Unburned Withdraw: ${it.id}")
            }
        }
        val withdraw = Withdraw(0, user, amount, ZonedDateTime.now(),
                null, null, null, null, null)
        return withdrawRepository.save(withdraw)
    }

    @Transactional
    override fun generateApprovalTransaction(withdrawId: Int, user: UUID): TransactionDataAndInfo {
        val withdraw = getWithdraw(withdrawId)
        // TODO: limit only for creating user
        validateWithdrawForApproval(withdraw)
        val userWallet = getUserWallet(withdraw.user)
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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    @Transactional
    override fun burn(signedTransaction: String, withdrawId: Int): Withdraw {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun validateWithdrawForApproval(withdraw: Withdraw) {
        if (withdraw.approvedTxHash != null) {
            throw InvalidRequestException(
                    ErrorCode.WALLET_WITHDRAW_APPROVED, "Approved txHash: ${withdraw.approvedTxHash}")
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
