package com.ampnet.crowdfundingbackend.controller.pojo.response

import com.ampnet.crowdfundingbackend.persistence.model.Wallet
import java.math.BigDecimal

data class WalletResponse(val id: Int, val balance: BigDecimal, val currency: String, val createdAt: String) {
    constructor(wallet: Wallet, balance: BigDecimal, createdAt: String)
            : this(wallet.id, balance, wallet.currency.name, createdAt)
}
