package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.persistence.model.Country
import com.ampnet.crowdfundingbackend.persistence.repository.CountryRepository
import com.ampnet.crowdfundingbackend.service.CountryService
import org.springframework.stereotype.Service

@Service
class CountryServiceImpl(private val countryRepository: CountryRepository) : CountryService {

    override fun getCountries(): List<Country> {
        return countryRepository.findAll()
    }

    override fun getCountry(nicename: String): Country? {
        val countryOptional = countryRepository.findByNicename(nicename)
        return ServiceUtils.wrapOptional(countryOptional)
    }

    override fun getCountry(id: Int): Country? {
        val countryOptional = countryRepository.findById(id)
        return ServiceUtils.wrapOptional(countryOptional)
    }
}
