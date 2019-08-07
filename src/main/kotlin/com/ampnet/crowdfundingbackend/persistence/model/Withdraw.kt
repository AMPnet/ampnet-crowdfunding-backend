package com.ampnet.crowdfundingbackend.persistence.model

import java.time.ZonedDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "withdraw")
data class Withdraw(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int,

    @Column(nullable = false)
    val user: UUID,

    @Column(nullable = false)
    val amount: Long,

    @Column(nullable = false)
    val createdAt: ZonedDateTime,

    @Column
    var approvedTxHash: String?,

    @Column
    var approvedAt: ZonedDateTime?,

    @Column
    var burnedTxHash: String?,

    @Column
    var burnedAt: ZonedDateTime?,

    @Column
    var burnedBy: UUID?
)
