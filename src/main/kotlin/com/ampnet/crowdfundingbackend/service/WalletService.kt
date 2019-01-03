package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.persistence.model.Wallet

interface WalletService {
    fun getWalletBalance(wallet: Wallet): Long
    fun createUserWallet(user: User, address: String): Wallet
    fun createProjectWallet(project: Project, address: String): Wallet
}
