package com.ampnet.crowdfundingbackend.blockchain.pojo

import com.ampnet.crowdfunding.proto.RawTxResponse
import com.ampnet.crowdfundingbackend.persistence.model.TransactionInfo

data class TransactionData(
    val data: String,
    val to: String,
    val nonce: Long,
    val gasLimit: Long,
    val gasPrice: Long,
    val value: Long,
    val publicKey: String
) {
    constructor(rawTxResponse: RawTxResponse) : this(
            rawTxResponse.data,
            rawTxResponse.to,
            rawTxResponse.nonce,
            rawTxResponse.gasLimit,
            rawTxResponse.gasPrice,
            rawTxResponse.value,
            rawTxResponse.publicKey
    )
}

data class TransactionDataAndInfo(
    val transactionData: TransactionData,
    val transactionInfo: TransactionInfo
)
