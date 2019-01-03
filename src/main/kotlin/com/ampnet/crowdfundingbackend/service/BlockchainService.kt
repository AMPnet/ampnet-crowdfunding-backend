package com.ampnet.crowdfundingbackend.service

import java.math.BigDecimal

interface BlockchainService {
    fun getBalance(address: String): BigDecimal
}
