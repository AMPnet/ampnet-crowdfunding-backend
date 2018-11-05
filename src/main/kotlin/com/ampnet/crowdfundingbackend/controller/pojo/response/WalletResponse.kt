package com.ampnet.crowdfundingbackend.controller.pojo.response

import com.ampnet.crowdfundingbackend.persistence.model.Wallet
import java.math.BigDecimal
import java.time.ZonedDateTime

data class WalletResponse(val id: Int, val balance: BigDecimal, val currency: String, val createdAt: ZonedDateTime) {
    constructor(wallet: Wallet, balance: BigDecimal)
            : this(wallet.id, balance, wallet.currency.name, wallet.createdAt)
}
