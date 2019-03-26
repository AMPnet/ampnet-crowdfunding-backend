package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.enums.TransactionType
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.TransactionInfo
import com.ampnet.crowdfundingbackend.persistence.repository.TransactionInfoRepository
import com.ampnet.crowdfundingbackend.service.TransactionInfoService
import com.ampnet.crowdfundingbackend.service.pojo.CreateTransactionRequest
import org.springframework.stereotype.Service

@Service
class TransactionInfoServiceImpl(
    private val transactionInfoRepository: TransactionInfoRepository
) : TransactionInfoService {

    private val createOrgTitle = "Create Organization"
    private val createOrgDescription = "You are signing transaction to create organization: %s"
    private val createProjectTitle = "Create Project"
    private val createProjectDescription = "You are signing transaction to create project: %s"
    private val investAllowanceTitle = "Invest Allowance"
    private val investAllowanceDescription = "You are signing transaction to allow investment to project: " +
            "%s with amount %.2f"
    private val investTitle = "Invest"
    private val investDescription = "You are signing transaction to confirm investment to project: %s"

    override fun createOrgTransaction(organization: Organization, userId: Int): TransactionInfo {
        val description = createOrgDescription.format(organization.name)
        val request = CreateTransactionRequest(
                TransactionType.CREATE_ORG, createOrgTitle, description, userId, organization.id)
        return createTransaction(request)
    }

    override fun createProjectTransaction(project: Project, userId: Int): TransactionInfo {
        val description = createProjectDescription.format(project.name)
        val request = CreateTransactionRequest(
                TransactionType.CREATE_PROJECT, createProjectTitle, description, userId, project.id)
        return createTransaction(request)
    }

    override fun createInvestAllowanceTransaction(projectName: String, amount: Long, userId: Int): TransactionInfo {
        val description = investAllowanceDescription.format(projectName, amount.toDouble().div(100))
        val request = CreateTransactionRequest(
                TransactionType.INVEST_ALLOWANCE, investAllowanceTitle, description, userId)
        return createTransaction(request)
    }

    override fun createInvestTransaction(projectName: String, userId: Int): TransactionInfo {
        val description = investDescription.format(projectName)
        val request = CreateTransactionRequest(
                TransactionType.INVEST, investTitle, description, userId)
        return createTransaction(request)
    }

    override fun deleteTransaction(id: Int) {
        transactionInfoRepository.deleteById(id)
    }

    override fun findTransactionInfo(id: Int): TransactionInfo? =
            ServiceUtils.wrapOptional(transactionInfoRepository.findById(id))

    private fun createTransaction(request: CreateTransactionRequest): TransactionInfo {
        val transaction = TransactionInfo::class.java.getDeclaredConstructor().newInstance().apply {
            type = request.type
            title = request.title
            description = request.description
            userId = request.userId
            companionId = request.companionId
        }
        return transactionInfoRepository.save(transaction)
    }
}
