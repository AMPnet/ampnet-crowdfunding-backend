package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.persistence.model.Withdraw
import java.util.UUID

interface WithdrawService {
    fun getWithdraws(approved: Boolean): List<Withdraw>
    fun createWithdraw(user: UUID, amount: Long): Withdraw
    fun approveWithdraw(user: UUID, withdrawId: Int, reference: String): Withdraw
}
