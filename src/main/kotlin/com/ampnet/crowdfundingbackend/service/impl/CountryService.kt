package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.persistence.model.Country
import com.ampnet.crowdfundingbackend.persistence.repository.CountryDao
import com.ampnet.crowdfundingbackend.service.CountryService
import org.springframework.stereotype.Service

@Service
class CountryService(private val countryDao: CountryDao) : CountryService {

    override fun getCountries(): List<Country> {
        return countryDao.findAll()
    }

    override fun getCountry(nicename: String): Country? {
        val countryOptional = countryDao.findByNicename(nicename)
        return ServiceUtils.wrapOptional(countryOptional)
    }

    override fun getCountry(id: Int): Country? {
        val countryOptional = countryDao.findById(id)
        return ServiceUtils.wrapOptional(countryOptional)
    }
}
