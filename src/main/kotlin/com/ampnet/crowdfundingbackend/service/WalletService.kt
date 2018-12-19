package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.persistence.model.Wallet
import java.math.BigDecimal

interface WalletService {
    fun getWalletBalance(wallet: Wallet): BigDecimal
    fun createUserWallet(user: User, address: String): Wallet
}
