package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.persistence.model.TransactionInfo

interface TransactionInfoService {
    fun createOrgTransaction(orgName: String, userId: Int): TransactionInfo
    fun createProjectTransaction(projectName: String, userId: Int): TransactionInfo
    fun createInvestAllowanceTransaction(projectName: String, amount: Long, userId: Int): TransactionInfo
    fun createInvestTransaction(projectName: String, userId: Int): TransactionInfo
    fun deleteTransaction(id: Int)
}
