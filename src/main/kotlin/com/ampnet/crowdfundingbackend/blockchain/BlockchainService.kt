package com.ampnet.crowdfundingbackend.blockchain

import com.ampnet.crowdfundingbackend.service.pojo.GenerateProjectWalletRequest
import com.ampnet.crowdfundingbackend.service.pojo.TransactionData

interface BlockchainService {
    fun getBalance(hash: String): Long?
    fun addWallet(address: String): String?
    fun generateAddOrganizationTransaction(userWalletHash: String, name: String): TransactionData
    fun generateProjectWalletTransaction(request: GenerateProjectWalletRequest): TransactionData
    fun postTransaction(transaction: String): String
    fun activateOrganization(organizationWalletHash: String): String
}
