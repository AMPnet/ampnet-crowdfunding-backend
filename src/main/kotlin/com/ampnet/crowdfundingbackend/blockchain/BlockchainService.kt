package com.ampnet.crowdfundingbackend.blockchain

import com.ampnet.crowdfundingbackend.service.pojo.GenerateProjectWalletRequest
import com.ampnet.crowdfundingbackend.service.pojo.PostTransactionType
import com.ampnet.crowdfundingbackend.service.pojo.TransactionData

interface BlockchainService {
    fun getBalance(hash: String): Long?
    fun addWallet(address: String, publicKey: String): String?
    fun generateAddOrganizationTransaction(userWalletHash: String, name: String): TransactionData
    fun generateProjectWalletTransaction(request: GenerateProjectWalletRequest): TransactionData
    fun postTransaction(transaction: String, type: PostTransactionType): String
    fun activateOrganization(organizationWalletHash: String): String
    fun generateInvestInProjectTransaction(
        userWalletHash: String,
        projectWalletHash: String,
        amount: Long
    ): TransactionData
}
