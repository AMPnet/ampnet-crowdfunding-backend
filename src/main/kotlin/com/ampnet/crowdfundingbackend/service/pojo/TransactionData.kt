package com.ampnet.crowdfundingbackend.service.pojo

import com.ampnet.crowdfunding.proto.RawTxResponse

data class TransactionData(
    val data: String,
    val to: String,
    val nonce: Long,
    val gasLimit: Long,
    val gasPrice: Long,
    val value: Long,
    val publicKey: String
) {
    constructor(rawTxResponse: RawTxResponse): this(
            rawTxResponse.data,
            rawTxResponse.to,
            rawTxResponse.nonce,
            rawTxResponse.gasLimit,
            rawTxResponse.gasPrice,
            rawTxResponse.value,
            rawTxResponse.publicKey
    )
}
