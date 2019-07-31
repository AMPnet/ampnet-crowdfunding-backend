package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.persistence.model.Withdraw
import com.ampnet.crowdfundingbackend.persistence.repository.UserWalletRepository
import com.ampnet.crowdfundingbackend.persistence.repository.WithdrawRepository
import com.ampnet.crowdfundingbackend.service.WithdrawService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

@Service
class WithdrawServiceImpl(
    private val withdrawRepository: WithdrawRepository,
    private val userWalletRepository: UserWalletRepository
) : WithdrawService {

    @Transactional(readOnly = true)
    override fun getWithdraws(approved: Boolean): List<Withdraw> {
        return withdrawRepository.findByApproved(approved)
    }

    @Transactional
    override fun createWithdraw(user: UUID, amount: Long): Withdraw {
        if (userWalletRepository.findByUserUuid(user).isPresent.not()) {
            throw ResourceNotFoundException(ErrorCode.WALLET_MISSING,
                    "User must have a wallet to make Withdraw request")
        }
        val withdraw = Withdraw(0, user, amount, false, null, null, null, ZonedDateTime.now())
        return withdrawRepository.save(withdraw)
    }

    @Transactional
    override fun approveWithdraw(user: UUID, withdrawId: Int, reference: String): Withdraw {
        val withdraw = withdrawRepository.findById(withdrawId).orElseThrow {
            throw ResourceNotFoundException(ErrorCode.WALLET_WITHDRAW_MISSING, "Missing withdraw with id: $withdrawId")
        }
        withdraw.approved = true
        withdraw.approvedAt = ZonedDateTime.now()
        withdraw.approvedByUserUuid = user
        withdraw.approvedReference = reference
        return withdrawRepository.save(withdraw)
    }
}
