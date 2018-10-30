package com.ampnet.crowdfundingbackend.persistence.model

import java.math.BigDecimal
import java.time.ZonedDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "transaction")
data class Transaction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int,

    @Column(nullable = false)
    var walletId: Int,

    @Column(nullable = false)
    var sender: String,

    @Column(nullable = false)
    var receiver: String,

    @Column(nullable = false)
    var amount: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    var currency: Currency,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    var type: TransactionType,

    @Column(nullable = false)
    var txHash: String,

    @Column(nullable = false)
    var timestamp: ZonedDateTime
)
