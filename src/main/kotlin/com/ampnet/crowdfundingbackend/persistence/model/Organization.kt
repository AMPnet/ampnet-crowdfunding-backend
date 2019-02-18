package com.ampnet.crowdfundingbackend.persistence.model

import org.hibernate.search.annotations.Field
import org.hibernate.search.annotations.Indexed
import org.hibernate.search.annotations.TermVector
import java.time.ZonedDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.JoinTable
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.Table

@Entity
@Indexed
@Table(name = "organization")
data class Organization(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int,

    @Column(nullable = false)
    @Field(termVector = TermVector.YES)
    var name: String,

    @Column(nullable = true)
    var legalInfo: String?,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    var createdByUser: User,

    @Column(nullable = false)
    var createdAt: ZonedDateTime,

    @Column(nullable = true)
    var updatedAt: ZonedDateTime?,

    @Column(nullable = false)
    var approved: Boolean,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    var approvedBy: User?,

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
