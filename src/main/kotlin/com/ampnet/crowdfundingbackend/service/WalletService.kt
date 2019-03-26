package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.controller.pojo.request.WalletCreateRequest
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.persistence.model.Wallet
import com.ampnet.crowdfundingbackend.service.pojo.TransactionDataAndInfo

interface WalletService {
    fun getWalletBalance(wallet: Wallet): Long
    fun createUserWallet(user: User, request: WalletCreateRequest): Wallet
    fun generateTransactionToCreateProjectWallet(project: Project, userId: Int): TransactionDataAndInfo
    fun createProjectWallet(project: Project, signedTransaction: String): Wallet
    fun generateTransactionToCreateOrganizationWallet(organization: Organization, userId: Int): TransactionDataAndInfo
    fun createOrganizationWallet(organization: Organization, signedTransaction: String): Wallet
}
