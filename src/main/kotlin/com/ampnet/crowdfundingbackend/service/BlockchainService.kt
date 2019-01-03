package com.ampnet.crowdfundingbackend.service

interface BlockchainService {
    fun getBalance(address: String): Long
}
