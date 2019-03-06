package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.blockchain.BlockchainService
import com.ampnet.crowdfundingbackend.blockchain.pojo.ProjectInvestmentTxRequest
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.InvalidRequestException
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.service.ProjectInvestmentService
import com.ampnet.crowdfundingbackend.service.WalletService
import com.ampnet.crowdfundingbackend.service.pojo.PostTransactionType
import com.ampnet.crowdfundingbackend.service.pojo.ProjectInvestmentRequest
import com.ampnet.crowdfundingbackend.service.pojo.TransactionData
import org.springframework.stereotype.Service
import java.time.ZonedDateTime

@Service
class ProjectInvestmentServiceImpl(
    private val walletService: WalletService,
    private val blockchainService: BlockchainService
) : ProjectInvestmentService {

    @Throws(InvalidRequestException::class, ResourceNotFoundException::class)
    override fun generateInvestInProjectTransaction(request: ProjectInvestmentRequest): TransactionData {
        verifyProjectIsStillActive(request.project)
        verifyInvestmentAmountIsValid(request.project, request.amount)
        verifyUserHasEnoughFunds(request.investor, request.amount)
        verifyProjectDidNotReachExpectedInvestment(request.project)

        val investRequest = ProjectInvestmentTxRequest(
            request.investor.wallet!!.hash,
            request.project.wallet!!.hash,
            request.amount
        )
        return blockchainService.generateProjectInvestmentTransaction(investRequest)
    }

    override fun investInProject(signedTransaction: String): String =
        blockchainService.postTransaction(signedTransaction, PostTransactionType.PRJ_INVEST)

    override fun generateConfirmInvestment(user: User, project: Project): TransactionData {
        val userWallet = getUserWallet(user)
        val projectWallet = getProjectWallet(project)

        return blockchainService.generateConfirmInvestment(userWallet.hash, projectWallet.hash)
    }

    override fun confirmInvestment(signedTransaction: String): String =
        blockchainService.postTransaction(signedTransaction, PostTransactionType.PRJ_INVEST_CONFIRM)

    private fun verifyProjectIsStillActive(project: Project) {
        if (project.active.not()) {
            throw InvalidRequestException(ErrorCode.PRJ_NOT_ACTIVE, "Project is not active")
        }
        if (project.endDate.isBefore(ZonedDateTime.now())) {
            throw InvalidRequestException(ErrorCode.PRJ_DATE_EXPIRED, "Project has expired at: ${project.endDate}")
        }
    }

    private fun verifyInvestmentAmountIsValid(project: Project, amount: Long) {
        if (amount > project.maxPerUser) {
            throw InvalidRequestException(ErrorCode.PRJ_MAX_PER_USER, "User can invest max ${project.maxPerUser}")
        }
        if (amount < project.minPerUser) {
            throw InvalidRequestException(
                    ErrorCode.PRJ_MIN_PER_USER, "User has to invest at least ${project.minPerUser}")
        }
    }

    private fun verifyUserHasEnoughFunds(user: User, amount: Long) {
        val wallet = getUserWallet(user)

        val funds = walletService.getWalletBalance(wallet)
        if (funds < amount) {
            throw InvalidRequestException(ErrorCode.WALLET_FUNDS, "User does not have enough funds on wallet")
        }
    }

    private fun verifyProjectDidNotReachExpectedInvestment(project: Project) {
        val wallet = getProjectWallet(project)

        val currentFunds = walletService.getWalletBalance(wallet)
        if (currentFunds == project.expectedFunding) {
            throw InvalidRequestException(
                    ErrorCode.PRJ_MAX_FUNDS, "Project has reached expected funding: $currentFunds")
        }
    }

    private fun getUserWallet(user: User) = user.wallet
        ?: throw ResourceNotFoundException(ErrorCode.WALLET_MISSING, "User does not have the wallet")

    private fun getProjectWallet(project: Project) = project.wallet
        ?: throw ResourceNotFoundException(ErrorCode.WALLET_MISSING, "Project does not have the wallet")
}
