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
@Table(name = "deposit")
data class Deposit(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int,

    @Column(nullable = false)
    val userUuid: UUID,

    @Column(nullable = false)
    val reference: String,

    @Column(nullable = false)
    var approved: Boolean,

    @Column(nullable = false)
    var amount: Long,

    @Column
    var approvedByUserUuid: UUID?,

    @Column
    var approvedAt: ZonedDateTime?,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    var document: Document?,

    @Column
    var txHash: String?,

    @Column(nullable = false)
    val createdAt: ZonedDateTime
)
