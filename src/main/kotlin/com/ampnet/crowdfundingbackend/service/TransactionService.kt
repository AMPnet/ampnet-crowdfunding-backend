package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.persistence.model.Transaction

interface TransactionService {
    fun createOrgTransaction(orgName: String, userId: Int): Transaction
    fun createProjectTransaction(projectName: String, userId: Int): Transaction
    fun createInvestAllowanceTransaction(projectName: String, amount: Long, userId: Int): Transaction
    fun createInvestTransaction(projectName: String, userId: Int): Transaction
    fun deleteTransaction(id: Int)
}
