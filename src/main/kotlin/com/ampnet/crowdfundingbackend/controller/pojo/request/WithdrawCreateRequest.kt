package com.ampnet.crowdfundingbackend.controller.pojo.request

data class WithdrawCreateRequest(
    val amount: Long,
    val bankAccountId: Int
)
