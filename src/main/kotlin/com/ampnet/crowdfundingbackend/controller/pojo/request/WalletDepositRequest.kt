package com.ampnet.crowdfundingbackend.controller.pojo.request

import java.math.BigDecimal

data class WalletDepositRequest(
    val amount: BigDecimal,
    val sender: String
)
