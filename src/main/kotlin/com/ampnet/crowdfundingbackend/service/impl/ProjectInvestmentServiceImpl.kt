package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.InvalidRequestException
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.service.ProjectInvestmentService
import com.ampnet.crowdfundingbackend.service.WalletService
import com.ampnet.crowdfundingbackend.service.pojo.ProjectInvestmentRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.ZonedDateTime

@Service
class ProjectInvestmentServiceImpl(private val walletService: WalletService) : ProjectInvestmentService {

    @Transactional
    @Throws(InvalidRequestException::class, ResourceNotFoundException::class)
    override fun investToProject(request: ProjectInvestmentRequest) {
        verifyProjectIsStillActive(request.project)
        verifyInvestmentAmountIsValid(request.project, request.amount)
        verifyUserHasEnoughFunds(request.investor, request.amount)
        verifyProjectDidNotReachExpectedInvestment(request.project)
        verifyUserDidNotReachMaximumInvestment(request)
    }

    private fun verifyProjectIsStillActive(project: Project) {
        if (project.active.not()) {
            throw InvalidRequestException(ErrorCode.PRJ_NOT_ACTIVE, "Project is not active")
        }
        if (project.endDate.isBefore(ZonedDateTime.now())) {
            throw InvalidRequestException(ErrorCode.PRJ_DATE_EXPIRED, "Project has expired at: ${project.endDate}")
        }
    }

    private fun verifyInvestmentAmountIsValid(project: Project, amount: BigDecimal) {
        if (amount > project.maxPerUser) {
            throw InvalidRequestException(ErrorCode.PRJ_MAX_PER_USER, "User can invest max ${project.maxPerUser}")
        }
        if (amount < project.minPerUser) {
            throw InvalidRequestException(
                    ErrorCode.PRJ_MIN_PER_USER, "User has to invest at least ${project.minPerUser}")
        }
    }

    private fun verifyUserHasEnoughFunds(user: User, amount: BigDecimal) {
        val wallet = user.wallet
                ?: throw ResourceNotFoundException(ErrorCode.WALLET_MISSING, "User does not have the wallet")

        val funds = walletService.getWalletBalance(wallet)
        if (funds < amount) {
            throw InvalidRequestException(ErrorCode.WALLET_FUNDS, "User does not have enough funds on wallet")
        }
    }

    private fun verifyProjectDidNotReachExpectedInvestment(project: Project) {
        val wallet = project.wallet
                ?: throw ResourceNotFoundException(ErrorCode.WALLET_MISSING, "Project does not have the wallet")

        val currentFunds = walletService.getWalletBalance(wallet)
        if (currentFunds == project.expectedFunding) {
            throw InvalidRequestException(
                    ErrorCode.PRJ_MAX_FUNDS, "Project has reached expected funding: $currentFunds")
        }
    }

    private fun verifyUserDidNotReachMaximumInvestment(request: ProjectInvestmentRequest) {
        // TODO: implement logic: fetch all user investments in current project
        val currentInvestment = BigDecimal.ZERO
        if ((currentInvestment + request.amount) > request.project.maxPerUser) {
            val maxInvestment = request.project.maxPerUser.minus(currentInvestment)
            throw InvalidRequestException(ErrorCode.PRJ_MAX_PER_USER, "User can invest max $maxInvestment")
        }
    }
}
