package com.ampnet.crowdfundingbackend.service.pojo

import com.ampnet.crowdfundingbackend.enums.TransactionType
import java.util.UUID

data class CreateTransactionRequest(
    val type: TransactionType,
    val title: String,
    val description: String,
    val userUuid: UUID,
    val companionId: Int? = null
)
