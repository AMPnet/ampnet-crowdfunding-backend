package com.ampnet.crowdfundingbackend.persistence.model

import com.ampnet.crowdfundingbackend.enums.Currency
import com.ampnet.crowdfundingbackend.persistence.HashArrayToStringConverter
import java.math.BigDecimal
import java.time.ZonedDateTime
import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.Embeddable
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
    var returnToInvestment: String,

    @Column(nullable = false)
    var startDate: ZonedDateTime,

    @Column(nullable = false)
    var endDate: ZonedDateTime,

    @Column(nullable = false)
    var expectedFunding: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    var currency: Currency,

    @Column(nullable = false)
    var minPerUser: BigDecimal,

    @Column(nullable = false)
    var maxPerUser: BigDecimal,

    @Column
    var mainImage: String?,

    @Column
    @Convert(converter = HashArrayToStringConverter::class)
    var gallery: List<String>?,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    var createdBy: User,

    @Column(nullable = false)
    var createdAt: ZonedDateTime,

    @Column(nullable = false)
    var active: Boolean,

    @ManyToMany
    @JoinTable(name = "project_document",
            joinColumns = [JoinColumn(name = "project_id")],
            inverseJoinColumns = [JoinColumn(name = "document_id")]
    )
    var documents: List<Document>?

    // TODO: add @OneToMany for document and investors, lazy
)
