package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.persistence.model.Wallet

interface WalletService {
    fun createWallet(ownerId: Int): Wallet
    fun getWalletForUser(userId: Int): Wallet?
}
