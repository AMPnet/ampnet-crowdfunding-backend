package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.exception.ResourceAlreadyExistsException
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.persistence.model.Currency
import com.ampnet.crowdfundingbackend.persistence.model.Transaction
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.persistence.model.Wallet
import com.ampnet.crowdfundingbackend.persistence.repository.TransactionRepository
import com.ampnet.crowdfundingbackend.persistence.repository.WalletRepository
import com.ampnet.crowdfundingbackend.service.UserService
import com.ampnet.crowdfundingbackend.service.WalletService
import com.ampnet.crowdfundingbackend.service.pojo.DepositRequest
import com.ampnet.crowdfundingbackend.service.pojo.TransactionRequest
import com.ampnet.crowdfundingbackend.service.pojo.TransferRequest
import com.ampnet.crowdfundingbackend.service.pojo.WithdrawRequest
import mu.KLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.ZonedDateTime

@Service
class WalletServiceImpl(
    private val walletRepository: WalletRepository,
    private val transactionRepository: TransactionRepository,
    private val userService: UserService
) : WalletService {

    companion object : KLogging()

    @Transactional(readOnly = true)
    override fun getWalletForUser(userId: Int): Wallet? {
        return ServiceUtils.wrapOptional(walletRepository.findByOwnerId(userId))
    }

    @Transactional(readOnly = true)
    override fun getWalletWithTransactionsForUser(userId: Int): Wallet? {
        return ServiceUtils.wrapOptional(walletRepository.findByOwnerIdWithTransactions(userId))
    }

    @Transactional(readOnly = true)
    override fun getWalletBalance(wallet: Wallet): BigDecimal {
        // TODO: get balance from blockchain, throw exception if it fails
        return BigDecimal.ZERO
    }

    @Transactional
    @Throws(ResourceAlreadyExistsException::class)
    override fun createWallet(ownerId: Int): Wallet {
        if (walletRepository.findByOwnerId(ownerId).isPresent) {
            logger.info("Trying to save wallet with ownerId: $ownerId which already exists in db.")
            throw ResourceAlreadyExistsException("Wallet with ownerId: $ownerId already exits")
        }

        val wallet = Wallet::class.java.newInstance()
        wallet.ownerId = ownerId
        wallet.currency = Currency.EUR
        wallet.createdAt = ZonedDateTime.now()
        wallet.transactions = emptyList()
        return walletRepository.save(wallet)
    }

    @Transactional
    @Throws(ResourceNotFoundException::class)
    override fun depositToWallet(request: DepositRequest): Transaction {
        val receiverName = getUsernameForWalletId(request.wallet)
        val transactionRequest = TransactionRequest(request, receiverName)
        return saveTransaction(transactionRequest)
    }

    @Transactional
    @Throws(ResourceNotFoundException::class)
    override fun withdrawFromWallet(request: WithdrawRequest): Transaction {
        val senderName = getUsernameForWalletId(request.wallet)
        val transactionRequest = TransactionRequest(request, senderName)
        return saveTransaction(transactionRequest)
    }

    @Transactional
    @Throws(ResourceNotFoundException::class)
    override fun transferFromWalletToWallet(request: TransferRequest): Transaction {
        val sender = userService.find(request.senderId)
        val receiver = userService.find(request.receiverId)
        if (sender == null || receiver == null) {
            logger.warn("Trying to transfer from or to non existing account. " +
                    "SenderId: ${request.senderId} and ReceiverId: ${request.receiverId}")
            throw ResourceNotFoundException("Cannot transferFromWalletToWallet. User is missing.")
        }

        val withdrawRequest = createWithdrawTransaction(sender, request, receiver.getFullName())
        val depositRequest = createDepositTransaction(receiver, request, sender.getFullName())

        val withdrawTransaction = withdrawFromWallet(withdrawRequest)
        depositToWallet(depositRequest)

        return withdrawTransaction
    }

    private fun createWithdrawTransaction(
        sender: User,
        transferRequest: TransferRequest,
        receiverName: String
    ): WithdrawRequest {
        val senderWallet = getWalletForUser(sender.id)
        if (senderWallet == null) {
            logger.warn("Trying to transfer founds from wallet. Missing wallet for user: ${sender.id}")
            throw ResourceNotFoundException("Missing wallet for user: ${sender.email}")
        }
        return WithdrawRequest(
                senderWallet,
                transferRequest.amount,
                transferRequest.currency,
                receiverName,
                transferRequest.txHash
        )
    }

    private fun createDepositTransaction(
        receiver: User,
        transferRequest: TransferRequest,
        senderName: String
    ): DepositRequest {
        val receiverWallet = getWalletForUser(receiver.id)
        if (receiverWallet == null) {
            logger.warn("Trying to transfer founds to wallet. Missing wallet for user: ${receiver.id}")
            throw ResourceNotFoundException("Missing wallet for user: ${receiver.email}")
        }
        return DepositRequest(
                receiverWallet,
                transferRequest.amount,
                transferRequest.currency,
                senderName,
                transferRequest.txHash
        )
    }

    private fun saveTransaction(request: TransactionRequest): Transaction {
        val transaction = Transaction::class.java.newInstance()
        transaction.timestamp = ZonedDateTime.now()
        transaction.currency = Currency.EUR
        transaction.walletId = request.walletId
        transaction.sender = request.sender
        transaction.receiver = request.receiver
        transaction.amount = request.amount
        transaction.txHash = request.txHash
        transaction.type = request.type

        return transactionRepository.save(transaction)
    }

    private fun getUsernameForWalletId(wallet: Wallet): String {
        val user = userService.find(wallet.ownerId)
                ?: throw ResourceNotFoundException("Missing user with id: ${wallet.ownerId}")
        return user.getFullName()
    }
}
