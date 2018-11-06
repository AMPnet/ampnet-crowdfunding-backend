package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.persistence.model.Transaction
import com.ampnet.crowdfundingbackend.persistence.model.Wallet
import com.ampnet.crowdfundingbackend.service.pojo.DepositRequest
import com.ampnet.crowdfundingbackend.service.pojo.TransferRequest
import com.ampnet.crowdfundingbackend.service.pojo.WithdrawRequest
import java.math.BigDecimal

interface WalletService {
    fun getWalletForUser(userId: Int): Wallet?
    fun getWalletWithTransactionsForUser(userId: Int): Wallet?
    fun getWalletBalance(wallet: Wallet): BigDecimal
    fun createWallet(ownerId: Int): Wallet
    fun depositToWallet(request: DepositRequest): Transaction
    fun withdrawFromWallet(request: WithdrawRequest): Transaction
    fun transferFromWalletToWallet(request: TransferRequest): Transaction
}
