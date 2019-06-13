package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.service.pojo.ProjectInvestmentRequest
import com.ampnet.crowdfundingbackend.service.pojo.TransactionDataAndInfo

interface ProjectInvestmentService {
    fun generateInvestInProjectTransaction(request: ProjectInvestmentRequest): TransactionDataAndInfo
    fun investInProject(signedTransaction: String): String
    fun generateConfirmInvestment(userUuid: String, project: Project): TransactionDataAndInfo
    fun confirmInvestment(signedTransaction: String): String
}
