package com.ampnet.crowdfundingbackend.controller.pojo.response

import com.ampnet.crowdfundingbackend.persistence.model.WalletToken
import java.time.ZonedDateTime

data class WalletTokenResponse(
    val token: String,
    val createdAt: ZonedDateTime
) {
    constructor(walletToken: WalletToken) : this(
            walletToken.token.toString(),
            walletToken.createdAt
    )
}
