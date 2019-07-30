package com.ampnet.crowdfundingbackend.controller.pojo.response

import com.ampnet.crowdfundingbackend.persistence.model.Withdraw
import com.ampnet.userservice.proto.UserResponse
import java.time.ZonedDateTime
import java.util.UUID

data class WithdrawResponse(
    val id: Int,
    val user: UUID,
    val amount: Long,
    val approved: Boolean,
    val approvedReference: String?,
    val approvedBy: UUID?,
    val approvedAt: ZonedDateTime?,
    val createdAt: ZonedDateTime
) {
    constructor(withdraw: Withdraw) : this (
        withdraw.id,
        withdraw.userUuid,
        withdraw.amount,
        withdraw.approved,
        withdraw.approvedReference,
        withdraw.approvedByUserUuid,
        withdraw.approvedAt,
        withdraw.createdAt
    )
}

data class WithdrawWithUserResponse(
    val id: Int,
    val user: UserControllerResponse?,
    val amount: Long,
    val approved: Boolean,
    val createdAt: ZonedDateTime,
    val userWallet: String?
) {
    constructor(withdraw: Withdraw, user: UserResponse?, userWallet: String?) : this(
        withdraw.id,
        if (user != null) UserControllerResponse(user) else null,
        withdraw.amount,
        withdraw.approved,
        withdraw.createdAt,
        userWallet
    )
}

data class WithdrawWithUserListResponse(val withdraws: List<WithdrawWithUserResponse>)
