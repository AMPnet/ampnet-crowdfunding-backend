package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.TransactionInfo

interface TransactionInfoService {
    fun createOrgTransaction(organization: Organization, userUuid: String): TransactionInfo
    fun createProjectTransaction(project: Project, userUuid: String): TransactionInfo
    fun createInvestAllowanceTransaction(projectName: String, amount: Long, userUuid: String): TransactionInfo
    fun createInvestTransaction(projectName: String, userUuid: String): TransactionInfo
    fun deleteTransaction(id: Int)
    fun findTransactionInfo(id: Int): TransactionInfo?
}
