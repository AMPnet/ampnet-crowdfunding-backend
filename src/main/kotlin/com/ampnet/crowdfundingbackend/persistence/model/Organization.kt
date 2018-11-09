package com.ampnet.crowdfundingbackend.persistence.model

import com.ampnet.crowdfundingbackend.persistence.HashArrayToStringConverter
import java.time.ZonedDateTime
import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(name = "organization")
data class Organization(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int,

    @ManyToOne
    @JoinColumn(name = "created_by")
    var createdByUser: User,

    @Column(nullable = false)
    var createdAt: ZonedDateTime,

    @Column(nullable = false)
    var approved: Boolean,

    @Column(nullable = false)
    var name: String,

    @Column                         // set nullable false
    var legalInfo: String,          // TODO: change legal info, try to use @Embeddable and @Embedded

    @Column(nullable = true)
    @Convert(converter = HashArrayToStringConverter::class)
    var documents: List<String>
)
