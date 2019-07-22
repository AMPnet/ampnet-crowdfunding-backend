package com.ampnet.crowdfundingbackend.controller.pojo.response

import com.ampnet.crowdfundingbackend.persistence.model.PairWalletCode

data class PairWalletResponse(
    val code: String,
    val address: String,
    val publicKey: String
) {
    constructor(pairWalletCode: PairWalletCode) : this(
        pairWalletCode.code,
        pairWalletCode.address,
        pairWalletCode.publicKey
    )
}
