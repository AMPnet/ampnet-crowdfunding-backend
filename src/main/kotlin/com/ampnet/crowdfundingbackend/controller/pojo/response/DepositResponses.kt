package com.ampnet.crowdfundingbackend.controller.pojo.response

import com.ampnet.crowdfundingbackend.persistence.model.Deposit
import com.ampnet.userservice.proto.UserResponse
import java.time.ZonedDateTime
import java.util.UUID

data class DepositResponse(
    val id: Int,
    val user: UUID,
    val reference: String,
    val approved: Boolean,
    val approvedAt: ZonedDateTime?,
    val amount: Long?,
    val documentResponse: DocumentResponse?,
    val txHash: String?,
    val createdAt: ZonedDateTime
) {
    constructor(deposit: Deposit) : this(
        deposit.id,
        deposit.userUuid,
        deposit.reference,
        deposit.approved,
        deposit.approvedAt,
        deposit.amount,
        deposit.document?.let { DocumentResponse(it) },
        deposit.txHash,
        deposit.createdAt
    )
}

data class DepositWithUserResponse(
    val id: Int,
    val user: UserControllerResponse?,
    val reference: String,
    val approved: Boolean,
    val approvedAt: ZonedDateTime?,
    val amount: Long?,
    val documentResponse: DocumentResponse?,
    val txHash: String?,
    val createdAt: ZonedDateTime
) {
    constructor(deposit: Deposit, userResponse: UserResponse?) : this(
        deposit.id,
        userResponse?.let { UserControllerResponse(it) },
        deposit.reference,
        deposit.approved,
        deposit.approvedAt,
        deposit.amount,
        deposit.document?.let { DocumentResponse(it) },
        deposit.txHash,
        deposit.createdAt
    )
}

data class DepositWithUserListResponse(val deposits: List<DepositWithUserResponse>)
