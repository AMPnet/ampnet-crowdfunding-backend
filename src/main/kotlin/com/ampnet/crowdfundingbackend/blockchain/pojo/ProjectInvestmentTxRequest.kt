package com.ampnet.crowdfundingbackend.blockchain.pojo

data class ProjectInvestmentTxRequest(
    val userWalletHash: String,
    val projectWalletHash: String,
    val amount: Long
)
