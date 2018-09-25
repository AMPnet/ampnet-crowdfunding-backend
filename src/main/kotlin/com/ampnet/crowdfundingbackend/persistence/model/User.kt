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
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(name = "app_user")
data class User(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int,

    @Column(nullable = false)
    var email: String,

    @Column
    var password: String?,

    @ManyToOne
    @JoinColumn(name = "role_id")
    var role: Role,

    @Column(nullable = false)
    var createdAt: ZonedDateTime,

    @Enumerated(EnumType.STRING)
    @Column(length = 8)
    var authMethod: AuthMethod

)