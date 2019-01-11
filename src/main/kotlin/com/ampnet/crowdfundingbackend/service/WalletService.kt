package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.controller.pojo.request.WalletCreateRequest
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.persistence.model.Wallet
import com.ampnet.crowdfundingbackend.persistence.model.WalletToken
import com.ampnet.crowdfundingbackend.service.pojo.TransactionData

interface WalletService {
    fun getWalletBalance(wallet: Wallet): Long
    fun createUserWallet(request: WalletCreateRequest): Wallet
    fun createWalletToken(user: User): WalletToken
    fun generateTransactionToCreateProjectWallet(project: Project): TransactionData
    fun createProjectWallet(project: Project, signedTransaction: String): Wallet
}
