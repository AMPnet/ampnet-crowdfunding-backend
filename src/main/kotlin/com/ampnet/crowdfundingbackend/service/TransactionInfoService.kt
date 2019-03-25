package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.TransactionInfo

interface TransactionInfoService {
    fun createOrgTransaction(organization: Organization, userId: Int): TransactionInfo
    fun createProjectTransaction(project: Project, userId: Int): TransactionInfo
    fun createInvestAllowanceTransaction(projectName: String, amount: Long, userId: Int): TransactionInfo
    fun createInvestTransaction(projectName: String, userId: Int): TransactionInfo
    fun deleteTransaction(id: Int)
    fun findTransactionInfo(id: Int): TransactionInfo?
}
