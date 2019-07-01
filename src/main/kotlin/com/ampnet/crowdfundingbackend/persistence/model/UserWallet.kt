package com.ampnet.crowdfundingbackend.persistence.model

import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToOne
import javax.persistence.Table

@Entity
@Table(name = "user_wallet")
data class UserWallet(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int,

    @Column(nullable = false)
    val userUuid: UUID,

    @OneToOne
    @JoinColumn(name = "wallet_id")
    val wallet: Wallet
)
