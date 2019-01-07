package com.ampnet.crowdfundingbackend.blockchain

interface BlockchainService {
    fun getBalance(address: String): Long
}
