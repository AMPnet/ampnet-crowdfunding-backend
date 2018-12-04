package com.ampnet.crowdfundingbackend.service.pojo

import com.ampnet.crowdfundingbackend.enums.Currency
import com.ampnet.crowdfundingbackend.enums.TransactionType
import com.ampnet.crowdfundingbackend.persistence.model.Wallet
import java.math.BigDecimal

data class DepositRequest(
    val wallet: Wallet,
    val amount: BigDecimal,
    val currency: Currency,
    val sender: String,
    val txHash: String
)

data class WithdrawRequest(
    val wallet: Wallet,
    val amount: BigDecimal,
    val currency: Currency,
    val receiver: String,
    val txHash: String
)

data class TransferRequest(
    val senderId: Int,
    val receiverId: Int,
    val amount: BigDecimal,
    val currency: Currency,
    val txHash: String
)

data class TransactionRequest(
    val walletId: Int,
    val sender: String,
    val receiver: String,
    val amount: BigDecimal,
    val txHash: String,
    val type: TransactionType
) {
    constructor(request: DepositRequest, receiver: String): this(
        request.wallet.id, request.sender, receiver, request.amount, request.txHash, TransactionType.DEPOSIT)

    constructor(request: WithdrawRequest, sender: String): this(
        request.wallet.id, sender, request.receiver, request.amount, request.txHash, TransactionType.WITHDRAW
    )
}
