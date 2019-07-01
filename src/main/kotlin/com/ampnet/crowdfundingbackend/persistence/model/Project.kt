package com.ampnet.crowdfundingbackend.persistence.model

import com.ampnet.crowdfundingbackend.enums.Currency
import com.ampnet.crowdfundingbackend.persistence.HashArrayToStringConverter
import java.time.ZonedDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.JoinTable
import javax.persistence.ManyToMany
import javax.persistence.ManyToOne
import javax.persistence.OneToOne
import javax.persistence.Table

@Entity
@Table(name = "project")
data class Project(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    var organization: Organization,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false)
    var description: String,

    @Column(nullable = false)
    var location: String,

    @Column(nullable = false)
    var locationText: String,

    @Column(nullable = false, length = 16)
    var returnOnInvestment: String,

    @Column(nullable = false)
    var startDate: ZonedDateTime,

    @Column(nullable = false)
    var endDate: ZonedDateTime,

    @Column(nullable = false)
    var expectedFunding: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    var currency: Currency,

    @Column(nullable = false)
    var minPerUser: Long,

    @Column(nullable = false)
    var maxPerUser: Long,

    @Column
    var mainImage: String?,

    @Column
    @Convert(converter = HashArrayToStringConverter::class)
    var gallery: List<String>?,

    @Column
    @Convert(converter = HashArrayToStringConverter::class)
    var newsLinks: List<String>?,

    @Column(nullable = true)
    var createdByUserUuid: UUID,

    @Column(nullable = false)
    var createdAt: ZonedDateTime,

    @Column(nullable = false)
    var active: Boolean,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id")
    var wallet: Wallet?,

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "project_document",
            joinColumns = [JoinColumn(name = "project_id")],
            inverseJoinColumns = [JoinColumn(name = "document_id")]
    )
    var documents: List<Document>?
)
