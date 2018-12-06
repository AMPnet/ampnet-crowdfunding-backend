package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.InvalidRequestException
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.persistence.repository.ProjectInvestmentRepository
import com.ampnet.crowdfundingbackend.service.ProjectInvestmentService
import com.ampnet.crowdfundingbackend.service.pojo.ProjectInvestmentRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.ZonedDateTime

@Service
class ProjectInvestmentServiceImpl(
    private val projectInvestmentRepository: ProjectInvestmentRepository
) : ProjectInvestmentService {

    @Transactional
    @Throws(InvalidRequestException::class)
    override fun investToProject(request: ProjectInvestmentRequest) {
        verifyProjectIsStillActive(request.project)
        verifyInvestmentAmountIsValid(request.project, request.amount)
        verifyProjectDidNotReachExpectedInvestment(request.project)
        verifyUserDidNotReachMaximumInvestment(request)
        verifyUserHasEnoughFunds(request.investor, request.amount)
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

    private fun verifyProjectDidNotReachExpectedInvestment(project: Project) {
        // TODO: fetch project funds from blockchain, project.walletAddress
        val currentFunds = BigDecimal.ONE
        if (currentFunds == project.expectedFunding) {
            throw InvalidRequestException(
                    ErrorCode.PRJ_MAX_FUNDS, "Project has reached expected founding: $currentFunds")
        }
    }

    private fun verifyUserDidNotReachMaximumInvestment(request: ProjectInvestmentRequest) {
        val allInvestmentsToProject = projectInvestmentRepository
                .findByProjectIdAndUserId(request.project.id, request.investor.id)
        val currentInvestment = allInvestmentsToProject
                // TODO: filter by transaction state, skip only failed or better create specific query
                .map { it.transaction.amount }
                .stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)

        if ((currentInvestment + request.amount) > request.project.maxPerUser) {
            val maxInvestment = request.project.maxPerUser.minus(currentInvestment)
            throw InvalidRequestException(ErrorCode.PRJ_MAX_PER_USER, "User can invest max $maxInvestment")
        }
    }

    private fun verifyUserHasEnoughFunds(user: User, amount: BigDecimal) {
        // TODO: fetch from blockchain
        val funds = BigDecimal.ZERO
        if (funds < amount) {
            throw InvalidRequestException(ErrorCode.WALLET_FOUNDS, "User does not have enough founds on wallet")
        }
    }
}
