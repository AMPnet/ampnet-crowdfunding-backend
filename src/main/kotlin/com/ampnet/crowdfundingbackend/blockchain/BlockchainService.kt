package com.ampnet.crowdfundingbackend.blockchain

import com.ampnet.crowdfundingbackend.blockchain.pojo.ProjectInvestmentTxRequest
import com.ampnet.crowdfundingbackend.blockchain.pojo.GenerateProjectWalletRequest
import com.ampnet.crowdfundingbackend.service.pojo.PostTransactionType
import com.ampnet.crowdfundingbackend.blockchain.pojo.TransactionData

interface BlockchainService {
    fun getBalance(hash: String): Long
    fun addWallet(address: String, publicKey: String): String
    fun generateAddOrganizationTransaction(userWalletHash: String, name: String): TransactionData
    fun generateProjectWalletTransaction(request: GenerateProjectWalletRequest): TransactionData
    fun postTransaction(transaction: String, type: PostTransactionType): String
    fun activateOrganization(organizationWalletHash: String): String
    fun generateProjectInvestmentTransaction(request: ProjectInvestmentTxRequest): TransactionData
    fun generateConfirmInvestment(userWalletHash: String, projectWalletHash: String): TransactionData
    fun generateMintTransaction(from: String, toHash: String, amount: Long): TransactionData
    fun generateBurnTransaction(from: String, burnFromTxHash: String, amount: Long): TransactionData
    fun generateApproveBurnTransaction(burnFromTxHash: String, amount: Long): TransactionData
}
