package com.ampnet.crowdfundingbackend.persistence.model

import javax.persistence.*

@Entity
@Table(name = "role")
data class Role(
        @Id
        val id: Int,

        @Column(nullable = false)
        val name: String,

        @Column(nullable = false)
        val description: String
)
