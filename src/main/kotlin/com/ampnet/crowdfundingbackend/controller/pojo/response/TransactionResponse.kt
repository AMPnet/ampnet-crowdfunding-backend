package com.ampnet.crowdfundingbackend.controller.pojo.response

import com.ampnet.crowdfundingbackend.service.pojo.TransactionData

data class TransactionResponse(
    val transactionData: TransactionData,
    val link: String
)
