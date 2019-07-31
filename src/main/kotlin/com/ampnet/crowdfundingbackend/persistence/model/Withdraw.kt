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
    val userUuid: UUID,

    @Column(nullable = false)
    val amount: Long,

    @Column(nullable = false)
    var approved: Boolean,

    @Column
    var approvedReference: String?,

    @Column
    var approvedByUserUuid: UUID?,

    @Column
    var approvedAt: ZonedDateTime?,

    @Column(nullable = false)
    val createdAt: ZonedDateTime
)
