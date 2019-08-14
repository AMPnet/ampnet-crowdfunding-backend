package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.blockchain.BlockchainService
import com.ampnet.crowdfundingbackend.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.InvalidRequestException
import com.ampnet.crowdfundingbackend.exception.ResourceAlreadyExistsException
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.persistence.model.Deposit
import com.ampnet.crowdfundingbackend.persistence.repository.DepositRepository
import com.ampnet.crowdfundingbackend.persistence.repository.UserWalletRepository
import com.ampnet.crowdfundingbackend.service.DepositService
import com.ampnet.crowdfundingbackend.service.MailService
import com.ampnet.crowdfundingbackend.service.StorageService
import com.ampnet.crowdfundingbackend.service.TransactionInfoService
import com.ampnet.crowdfundingbackend.service.pojo.ApproveDepositRequest
import com.ampnet.crowdfundingbackend.service.pojo.MintServiceRequest
import com.ampnet.crowdfundingbackend.service.pojo.PostTransactionType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

@Service
class DepositServiceImpl(
    private val depositRepository: DepositRepository,
    private val walletRepository: UserWalletRepository,
    private val blockchainService: BlockchainService,
    private val transactionInfoService: TransactionInfoService,
    private val storageService: StorageService,
    private val mailService: MailService
) : DepositService {

    private val charPool: List<Char> = ('A'..'Z') + ('0'..'9')
    // TODO: remove after changing blockchain-service
    private val mintAccount = "0x43b0d9b605e68a0c50dc436757a86c82d97787cc"

    @Transactional
    override fun create(user: UUID, amount: Long): Deposit {
        if (walletRepository.findByUserUuid(user).isPresent.not()) {
            throw ResourceNotFoundException(ErrorCode.WALLET_MISSING, "User must have a wallet to create a Deposit")
        }
        val unapprovedDeposits = depositRepository.findByUserUuid(user).filter { it.approved.not() }
        if (unapprovedDeposits.isEmpty().not()) {
            throw ResourceAlreadyExistsException(ErrorCode.WALLET_DEPOSIT_EXISTS,
                    "Check your unapproved deposit: ${unapprovedDeposits.firstOrNull()?.id}")
        }

        val deposit = Deposit(0, user, generateDepositReference(), false, amount,
            null, null, null, null, ZonedDateTime.now()
        )
        depositRepository.save(deposit)
        mailService.sendDepositRequest(user, amount)
        return deposit
    }

    @Transactional
    override fun delete(id: Int) {
        val deposit = getDepositForId(id)
        if (deposit.txHash != null) {
            throw InvalidRequestException(ErrorCode.WALLET_DEPOSIT_MINTED, "Cannot delete minted deposit")
        }
        mailService.sendDepositInfo(deposit.userUuid, false)
        depositRepository.delete(deposit)
    }

    @Transactional
    override fun approve(request: ApproveDepositRequest): Deposit {
        val deposit = depositRepository.findById(request.id).orElseThrow {
            throw ResourceNotFoundException(ErrorCode.WALLET_DEPOSIT_MISSING, "Missing deposit: ${request.id}")
        }
        // TODO: think about document reading restrictions
        val document = storageService.saveDocument(request.documentSaveRequest)

        deposit.approved = true
        deposit.approvedByUserUuid = request.user
        deposit.approvedAt = ZonedDateTime.now()
        deposit.amount = request.amount
        deposit.document = document
        return depositRepository.save(deposit)
    }

    @Transactional(readOnly = true)
    override fun getAllWithDocuments(approved: Boolean): List<Deposit> {
        return depositRepository.findAllWithDocument(approved)
    }

    @Transactional(readOnly = true)
    override fun findByReference(reference: String): Deposit? {
        return ServiceUtils.wrapOptional(depositRepository.findByReference(reference))
    }

    @Transactional(readOnly = true)
    override fun getPendingForUser(user: UUID): Deposit? {
        return depositRepository.findByUserUuid(user).find { it.approved.not() }
    }

    @Transactional
    override fun generateMintTransaction(request: MintServiceRequest): TransactionDataAndInfo {
        val deposit = getDepositForId(request.depositId)
        validateDepositForMintTransaction(deposit)
        val amount = deposit.amount
        val receivingWallet = getUserWalletHash(deposit)
        val data = blockchainService.generateMintTransaction(mintAccount, receivingWallet, amount)
        val info = transactionInfoService.createMintTransaction(request, receivingWallet)
        return TransactionDataAndInfo(data, info)
    }

    @Transactional
    override fun confirmMintTransaction(signedTransaction: String, depositId: Int): Deposit {
        val deposit = getDepositForId(depositId)
        validateDepositForMintTransaction(deposit)
        val txHash = blockchainService.postTransaction(signedTransaction, PostTransactionType.ISSUER_MINT)
        deposit.txHash = txHash
        depositRepository.save(deposit)
        mailService.sendDepositInfo(deposit.userUuid, true)
        return deposit
    }

    private fun validateDepositForMintTransaction(deposit: Deposit) {
        if (deposit.approved.not()) {
            throw InvalidRequestException(ErrorCode.WALLET_DEPOSIT_NOT_APPROVED,
                    "Deposit: ${deposit.id} is not approved")
        }
        if (deposit.txHash != null) {
            throw ResourceAlreadyExistsException(ErrorCode.WALLET_DEPOSIT_MINTED, "Mint txHash: ${deposit.txHash}")
        }
    }

    private fun getDepositForId(depositId: Int): Deposit {
        return depositRepository.findById(depositId).orElseThrow {
            throw ResourceNotFoundException(ErrorCode.WALLET_DEPOSIT_MISSING,
                    "For mint transaction missing deposit: $depositId")
        }
    }

    private fun getUserWalletHash(deposit: Deposit): String {
        val userWallet = walletRepository.findByUserUuid(deposit.userUuid).orElseThrow {
            throw ResourceNotFoundException(ErrorCode.WALLET_MISSING,
                    "User: ${deposit.userUuid} does not have a wallet")
        }
        return userWallet.wallet.hash
    }

    private fun generateDepositReference(): String = (1..8)
        .map { kotlin.random.Random.nextInt(0, charPool.size) }
        .map(charPool::get)
        .joinToString("")
}
