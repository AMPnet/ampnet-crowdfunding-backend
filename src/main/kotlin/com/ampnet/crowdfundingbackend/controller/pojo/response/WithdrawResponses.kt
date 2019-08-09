package com.ampnet.crowdfundingbackend.controller.pojo.response

import com.ampnet.crowdfundingbackend.persistence.model.Withdraw
import com.ampnet.userservice.proto.UserResponse
import java.time.ZonedDateTime
import java.util.UUID

data class WithdrawResponse(
    val id: Int,
    val user: UUID,
    val amount: Long,
    val approvedTxHash: String?,
    val approvedAt: ZonedDateTime?,
    val burnedTxHash: String?,
    val burnedBy: UUID?,
    val burnedAt: ZonedDateTime?,
    val bankAccount: String,
    val createdAt: ZonedDateTime
) {
    constructor(withdraw: Withdraw) : this (
        withdraw.id,
        withdraw.userUuid,
        withdraw.amount,
        withdraw.approvedTxHash,
        withdraw.approvedAt,
        withdraw.burnedTxHash,
        withdraw.burnedBy,
        withdraw.burnedAt,
        withdraw.bankAccount,
        withdraw.createdAt
    )
}

data class WithdrawWithUserResponse(
    val id: Int,
    val user: UserControllerResponse?,
    val amount: Long,
    val approvedTxHash: String?,
    val approvedAt: ZonedDateTime?,
    val burnedTxHash: String?,
    val burnedBy: UUID?,
    val burnedAt: ZonedDateTime?,
    val createdAt: ZonedDateTime,
    val bankAccount: String,
    val userWallet: String
) {
    constructor(withdraw: Withdraw, user: UserResponse?, userWallet: String) : this(
        withdraw.id,
        user?.let { UserControllerResponse(it) },
        withdraw.amount,
        withdraw.approvedTxHash,
        withdraw.approvedAt,
        withdraw.burnedTxHash,
        withdraw.burnedBy,
        withdraw.burnedAt,
        withdraw.createdAt,
        withdraw.bankAccount,
        userWallet
    )
}

data class WithdrawWithUserListResponse(val withdraws: List<WithdrawWithUserResponse>)
