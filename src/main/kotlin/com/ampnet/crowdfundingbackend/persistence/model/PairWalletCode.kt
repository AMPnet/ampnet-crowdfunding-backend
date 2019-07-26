package com.ampnet.crowdfundingbackend.persistence.model

import java.time.ZonedDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "pair_wallet_code")
data class PairWalletCode(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int,

    @Column(nullable = false, length = 66)
    var address: String,

    @Column(nullable = false)
    var publicKey: String,

    @Column(nullable = false, length = 6)
    var code: String,

    @Column(nullable = false)
    var createdAt: ZonedDateTime
)
