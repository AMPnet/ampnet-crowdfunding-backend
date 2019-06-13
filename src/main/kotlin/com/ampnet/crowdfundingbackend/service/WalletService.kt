package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.controller.pojo.request.WalletCreateRequest
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.Wallet
import com.ampnet.crowdfundingbackend.service.pojo.TransactionDataAndInfo

interface WalletService {
    fun getWalletBalance(wallet: Wallet): Long
    fun getUserWallet(userUuid: String): Wallet?
    fun createUserWallet(userUuid: String, request: WalletCreateRequest): Wallet
    fun generateTransactionToCreateProjectWallet(project: Project, userUuid: String): TransactionDataAndInfo
    fun createProjectWallet(project: Project, signedTransaction: String): Wallet
    fun generateTransactionToCreateOrganizationWallet(
            organization: Organization, userUuid: String): TransactionDataAndInfo
    fun createOrganizationWallet(organization: Organization, signedTransaction: String): Wallet
}
