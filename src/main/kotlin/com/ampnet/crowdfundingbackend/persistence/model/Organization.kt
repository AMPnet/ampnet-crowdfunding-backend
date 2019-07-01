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
import javax.persistence.JoinTable
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.Table

@Entity
@Table(name = "organization")
data class Organization(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = true)
    var legalInfo: String?,

    @Column(nullable = true)
    var createdByUserUuid: UUID,

    @Column(nullable = false)
    var createdAt: ZonedDateTime,

    @Column(nullable = true)
    var updatedAt: ZonedDateTime?,

    @Column(nullable = false)
    var approved: Boolean,

    @Column
    var approvedByUserUuid: UUID?,

    @OneToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "organization_document",
            joinColumns = [JoinColumn(name = "organization_id")],
            inverseJoinColumns = [JoinColumn(name = "document_id")]
    )
    var documents: List<Document>?,

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizationId")
    var memberships: List<OrganizationMembership>?,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id")
    var wallet: Wallet?
)
