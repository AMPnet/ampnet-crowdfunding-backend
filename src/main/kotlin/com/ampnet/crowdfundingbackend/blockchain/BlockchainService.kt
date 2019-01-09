package com.ampnet.crowdfundingbackend.blockchain

import com.ampnet.crowdfundingbackend.service.pojo.GenerateProjectWalletRequest
import com.ampnet.crowdfundingbackend.service.pojo.TransactionData

interface BlockchainService {
    fun getBalance(address: String): Long?
    fun addWallet(address: String): String?
    fun generateProjectWalletTransaction(request: GenerateProjectWalletRequest): TransactionData
    fun postTransaction(transaction: String): String
}
