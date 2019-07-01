package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.TransactionInfo
import java.util.UUID

interface TransactionInfoService {
    fun createOrgTransaction(organization: Organization, userUuid: UUID): TransactionInfo
    fun createProjectTransaction(project: Project, userUuid: UUID): TransactionInfo
    fun createInvestAllowanceTransaction(projectName: String, amount: Long, userUuid: UUID): TransactionInfo
    fun createInvestTransaction(projectName: String, userUuid: UUID): TransactionInfo
    fun deleteTransaction(id: Int)
    fun findTransactionInfo(id: Int): TransactionInfo?
}
