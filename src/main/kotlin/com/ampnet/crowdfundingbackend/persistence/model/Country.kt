package com.ampnet.crowdfundingbackend.persistence.model

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "country")
data class Country(
    @Id

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int,

    @Column(nullable = false)
    val iso: String,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false)
    val nicename: String,

    @Column
    val iso3: String?,

    @Column
    val numcode: Short?,

    @Column(nullable = false)
    val phonecode: Int
)