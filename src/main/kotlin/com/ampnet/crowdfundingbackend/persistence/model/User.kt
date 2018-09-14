package com.ampnet.crowdfundingbackend.persistence.model

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import javax.persistence.*

@Entity
@Table(name = "app_user")
data class User (

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int,

    @Column(nullable = false)
    var username: String,

    @Column(nullable = false)
    var password: String,

    @ManyToOne
    @JoinColumn(name = "role_id")
    var role: Role,

    @Column(nullable = false)
    var createdAt: ZonedDateTime

)