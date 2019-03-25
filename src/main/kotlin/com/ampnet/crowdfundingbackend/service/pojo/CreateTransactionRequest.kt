package com.ampnet.crowdfundingbackend.service.pojo

import com.ampnet.crowdfundingbackend.enums.TransactionType

data class CreateTransactionRequest(
    val type: TransactionType,
    val title: String,
    val description: String,
    val userId: Int
)
