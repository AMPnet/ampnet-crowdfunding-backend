package com.ampnet.crowdfundingbackend.controller.pojo.response

import com.ampnet.crowdfundingbackend.persistence.model.Transaction
import com.ampnet.crowdfundingbackend.persistence.model.Wallet
import java.math.BigDecimal
import java.time.ZonedDateTime

data class WalletResponse(
    val id: Int,
    val balance: BigDecimal,
    val currency: String,
    val transactions: List<TransactionResponse>,
    val createdAt: ZonedDateTime
) {
    constructor(wallet: Wallet, balance: BigDecimal) : this(
        wallet.id,
        balance,
        wallet.currency.name,
        wallet.transactions.map { TransactionResponse(it) },
        wallet.createdAt
    )
}

data class TransactionResponse(
    val id: Int,
    val sender: String,
    val receiver: String,
    val amount: BigDecimal,
    val currency: String,
    val type: String,
    val txHash: String,
    val timestamp: ZonedDateTime
) {
    constructor(transaction: Transaction): this(
        transaction.id,
        transaction.sender,
        transaction.receiver,
        transaction.amount,
        transaction.currency.name,
        transaction.type.name,
        transaction.txHash,
        transaction.timestamp
    )
}
