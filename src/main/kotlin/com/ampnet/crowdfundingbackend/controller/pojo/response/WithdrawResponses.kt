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
    val userWallet: String
) {
    constructor(withdraw: Withdraw, user: UserResponse?, userWallet: String) : this(
        withdraw.id,
        user?.let { UserControllerResponse(it) },
        withdraw.amount,
        withdraw.approved,
        withdraw.createdAt,
        userWallet
    )
}

data class WithdrawWithUserAndAcceptanceResponse(
    val id: Int,
    val user: UserControllerResponse?,
    val amount: Long,
    val approved: Boolean,
    val approvedBy: UserControllerResponse?,
    val approvedReference: String,
    val approvedAt: ZonedDateTime?,
    val createdAt: ZonedDateTime,
    val userWallet: String
) {
    constructor(withdraw: Withdraw, user: UserResponse?, approvedBy: UserResponse?, userWallet: String) : this(
        withdraw.id,
        user?.let { UserControllerResponse(it) },
        withdraw.amount,
        withdraw.approved,
        approvedBy?.let { UserControllerResponse(it) },
        withdraw.approvedReference.orEmpty(),
        withdraw.approvedAt,
        withdraw.createdAt,
        userWallet
    )
}

data class WithdrawWithUserListResponse(val withdraws: List<WithdrawWithUserResponse>)
data class WithdrawWithUserAndAcceptanceListResponse(val withdraws: List<WithdrawWithUserAndAcceptanceResponse>)
