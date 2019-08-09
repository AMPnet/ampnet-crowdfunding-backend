package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.crowdfundingbackend.persistence.model.Withdraw
import java.util.UUID

interface WithdrawService {
    fun getPendingForUser(user: UUID): Withdraw?
    fun getAllApproved(): List<Withdraw>
    fun getAllBurned(): List<Withdraw>
    fun createWithdraw(user: UUID, amount: Long, bankAccountId: Int): Withdraw
    fun deleteWithdraw(withdrawId: Int)
    fun generateApprovalTransaction(withdrawId: Int, user: UUID): TransactionDataAndInfo
    fun confirmApproval(signedTransaction: String, withdrawId: Int): Withdraw
    fun generateBurnTransaction(withdrawId: Int, user: UUID): TransactionDataAndInfo
    fun burn(signedTransaction: String, withdrawId: Int): Withdraw
}
