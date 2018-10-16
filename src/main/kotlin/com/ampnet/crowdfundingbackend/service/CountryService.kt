package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.persistence.model.Country

interface CountryService {
    fun getCountries(): List<Country>
    fun getCountry(nicename: String): Country?
    fun getCountry(id: Int): Country?
}
