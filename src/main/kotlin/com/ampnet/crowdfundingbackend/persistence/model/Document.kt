package com.ampnet.crowdfundingbackend.persistence.model

import com.ampnet.crowdfundingbackend.enums.DocumentType
import java.time.ZonedDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(name = "document")
data class Document(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int,

    @Column(nullable = false)
    var hash: String,

    @Column(nullable = false)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    var type: DocumentType,

    @Column(nullable = false)
    var size: Int,

    @ManyToOne
    @JoinColumn(name = "created_by")
    var createdBy: User?,

    @Column(nullable = false)
    var createdAt: ZonedDateTime
)
