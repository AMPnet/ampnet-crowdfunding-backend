package com.ampnet.crowdfundingbackend.controller.pojo.request

import com.ampnet.crowdfundingbackend.validation.WalletAddressConstraint

data class WalletCreateRequest(
    @WalletAddressConstraint
    val address: String,

    val publicKey: String
)
