package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.service.pojo.ProjectInvestmentRequest
import com.ampnet.crowdfundingbackend.service.pojo.TransactionData

interface ProjectInvestmentService {
    fun generateInvestInProjectTransaction(request: ProjectInvestmentRequest): TransactionData
    fun investInProject(signedTransaction: String): String
    fun generateConfirmInvestment(user: User, project: Project): TransactionData
    fun confirmInvestment(signedTransaction: String): String
}
