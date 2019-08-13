package com.ampnet.crowdfundingbackend.persistence.model

import java.time.ZonedDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToOne
import javax.persistence.Table

@Entity
@Table(name = "withdraw")
data class Withdraw(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int,

    @Column(nullable = false)
    val userUuid: UUID,

    @Column(nullable = false)
    val amount: Long,

    @Column(nullable = false)
    val createdAt: ZonedDateTime,

    @Column(nullable = false, length = 64)
    val bankAccount: String,

    @Column
    var approvedTxHash: String?,

    @Column
    var approvedAt: ZonedDateTime?,

    @Column
    var burnedTxHash: String?,

    @Column
    var burnedAt: ZonedDateTime?,

    @Column
    var burnedBy: UUID?,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    var document: Document?
)
