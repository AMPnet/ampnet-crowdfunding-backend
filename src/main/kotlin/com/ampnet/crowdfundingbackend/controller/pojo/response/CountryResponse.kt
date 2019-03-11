package com.ampnet.crowdfundingbackend.controller.pojo.response

import com.ampnet.crowdfundingbackend.persistence.model.Country

data class CountryResponse(
    val id: Int,
    val iso: String,
    val name: String,
    val nicename: String,
    val iso3: String?,
    val numcode: Short?,
    val phonecode: Int
) {
    constructor(country: Country) : this (
            country.id,
            country.iso,
            country.name,
            country.nicename,
            country.iso3,
            country.numcode,
            country.phonecode
    )
}

data class CountriesListResponse(val countries: List<CountryResponse>)
