package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.service.pojo.ProjectInvestmentRequest
import com.ampnet.crowdfundingbackend.blockchain.pojo.TransactionDataAndInfo
import java.util.UUID

interface ProjectInvestmentService {
    fun generateInvestInProjectTransaction(request: ProjectInvestmentRequest): TransactionDataAndInfo
    fun investInProject(signedTransaction: String): String
    fun generateConfirmInvestment(userUuid: UUID, project: Project): TransactionDataAndInfo
    fun confirmInvestment(signedTransaction: String): String
}
