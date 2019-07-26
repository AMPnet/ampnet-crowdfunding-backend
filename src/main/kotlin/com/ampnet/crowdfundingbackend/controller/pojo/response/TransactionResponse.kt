package com.ampnet.crowdfundingbackend.controller.pojo.response

import com.ampnet.crowdfundingbackend.enums.TransactionType
import com.ampnet.crowdfundingbackend.persistence.model.TransactionInfo
import com.ampnet.crowdfundingbackend.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.crowdfundingbackend.blockchain.pojo.TransactionData

data class TransactionResponse(
    val tx: TransactionData,
    val txId: Int,
    val info: TransactionInfoResponse
) {
    constructor(transaction: TransactionDataAndInfo) : this(
        transaction.transactionData,
        transaction.transactionInfo.id,
        TransactionInfoResponse(transaction.transactionInfo)
    )
}

data class TransactionInfoResponse(
    val txType: TransactionType,
    val title: String,
    val description: String
) {
    constructor(transactionInfo: TransactionInfo) : this(
        transactionInfo.type,
        transactionInfo.title,
        transactionInfo.description
    )
}

data class TransactionAndLinkResponse(val tx: TransactionData, val link: String)
