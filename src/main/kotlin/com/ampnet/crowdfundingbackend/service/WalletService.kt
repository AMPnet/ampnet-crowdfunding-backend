package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.controller.pojo.request.WalletCreateRequest
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.Wallet
import com.ampnet.crowdfundingbackend.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.crowdfundingbackend.persistence.model.PairWalletCode
import java.util.UUID

interface WalletService {
    fun getWalletBalance(wallet: Wallet): Long
    fun getUserWallet(userUuid: UUID): Wallet?
    fun createUserWallet(userUuid: UUID, request: WalletCreateRequest): Wallet
    fun generateTransactionToCreateProjectWallet(project: Project, userUuid: UUID): TransactionDataAndInfo
    fun createProjectWallet(project: Project, signedTransaction: String): Wallet
    fun generateTransactionToCreateOrganizationWallet(
        organization: Organization,
        userUuid: UUID
    ): TransactionDataAndInfo
    fun createOrganizationWallet(organization: Organization, signedTransaction: String): Wallet
    fun generatePairWalletCode(request: WalletCreateRequest): PairWalletCode
    fun getPairWalletCode(code: String): PairWalletCode?
}
