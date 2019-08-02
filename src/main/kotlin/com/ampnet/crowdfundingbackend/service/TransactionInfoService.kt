package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.TransactionInfo
import com.ampnet.crowdfundingbackend.service.pojo.MintServiceRequest
import java.util.UUID

interface TransactionInfoService {
    fun createOrgTransaction(organization: Organization, userUuid: UUID): TransactionInfo
    fun createProjectTransaction(project: Project, userUuid: UUID): TransactionInfo
    fun createInvestAllowanceTransaction(projectName: String, amount: Long, userUuid: UUID): TransactionInfo
    fun createInvestTransaction(projectName: String, userUuid: UUID): TransactionInfo
    fun createMintTransaction(request: MintServiceRequest): TransactionInfo
    fun deleteTransaction(id: Int)
    fun findTransactionInfo(id: Int): TransactionInfo?
}
