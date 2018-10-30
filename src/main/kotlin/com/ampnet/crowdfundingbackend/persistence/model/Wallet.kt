package com.ampnet.crowdfundingbackend.persistence.model

import java.time.ZonedDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToMany
import javax.persistence.Table

@Entity
@Table(name = "wallet")
data class Wallet(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int,

    @Column(nullable = false)
    var ownerId: Int,

//    @Column(nullable = false)
//    var balance: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    var currency: Currency,

//    @Column(nullable = false)
//    var address: String,

    @OneToMany // (mappedBy = "wallet")
    @JoinColumn(name = "walletId")
    var transactions: List<Transaction>,

    @Column(nullable = false)
    var createdAt: ZonedDateTime
)
