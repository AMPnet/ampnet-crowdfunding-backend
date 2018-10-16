package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.response.CountriesListResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.CountryResponse
import com.ampnet.crowdfundingbackend.service.CountryService
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class CountryController(private val countryService: CountryService) {

    companion object : KLogging()

    @GetMapping("/countries")
    fun getCountries(): ResponseEntity<CountriesListResponse> {
        logger.debug { "Received request to get all countries." }
        val countries = countryService.getCountries().map { it -> CountryResponse(it) }
        return ResponseEntity.ok(CountriesListResponse(countries))
    }

    @GetMapping("/countries/{id}")
    fun getCountry(@PathVariable("id") id: Int): ResponseEntity<CountryResponse> {
        val country = countryService.getCountry(id)
        return country?.let { ResponseEntity.ok(CountryResponse(it)) }
                ?: ResponseEntity.noContent().build()
    }
}
