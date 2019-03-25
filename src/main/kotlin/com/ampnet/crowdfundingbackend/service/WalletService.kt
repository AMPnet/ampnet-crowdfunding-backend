package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.controller.pojo.request.WalletCreateRequest
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.persistence.model.Wallet
import com.ampnet.crowdfundingbackend.service.pojo.TransactionData

interface WalletService {
    fun getWalletBalance(wallet: Wallet): Long
    fun createUserWallet(user: User, request: WalletCreateRequest): Wallet
    fun generateTransactionToCreateProjectWallet(project: Project, userId: Int): TransactionData
    fun createProjectWallet(project: Project, signedTransaction: String): Wallet
    fun generateTransactionToCreateOrganizationWallet(organization: Organization, userId: Int): TransactionData
    fun createOrganizationWallet(organization: Organization, signedTransaction: String): Wallet
}
