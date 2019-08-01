package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.persistence.model.Deposit
import com.ampnet.crowdfundingbackend.persistence.repository.DepositRepository
import com.ampnet.crowdfundingbackend.persistence.repository.UserWalletRepository
import com.ampnet.crowdfundingbackend.service.DepositService
import com.ampnet.crowdfundingbackend.service.StorageService
import com.ampnet.crowdfundingbackend.service.pojo.ApproveDepositRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

@Service
class DepositServiceImpl(
    private val depositRepository: DepositRepository,
    private val walletRepository: UserWalletRepository,
    private val storageService: StorageService
) : DepositService {

    private val charPool: List<Char> = ('A'..'Z') + ('0'..'9')

    @Transactional
    override fun create(user: UUID): Deposit {
        if (walletRepository.findByUserUuid(user).isPresent.not()) {
            throw ResourceNotFoundException(ErrorCode.WALLET_MISSING, "User must have a wallet to create a Deposit")
        }
        val deposit = Deposit(0, user, generateDepositReference(), false,
            null, null, null, null, ZonedDateTime.now()
        )
        return depositRepository.save(deposit)
    }

    @Transactional
    override fun delete(id: Int) {
        depositRepository.deleteById(id)
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

    private fun generateDepositReference(): String = (1..8)
        .map { kotlin.random.Random.nextInt(0, charPool.size) }
        .map(charPool::get)
        .joinToString("")
}
