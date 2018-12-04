package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.response.CountriesListResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.CountryResponse
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class CountryControllerTest : ControllerTestBase() {

    private val pathCountries = "/countries"
    private val croatiaId = 3

    @Test
    fun mustBeAbleToGetAListOfAllCountries() {
        suppose("Countries exists in database") {
            // 5 base countries are stored in database
        }

        verify("The system returns a list of countries") {
            val result = mockMvc.perform(get(pathCountries))
                    .andExpect(status().isOk)
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andReturn()

            val countriesResponse: CountriesListResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(countriesResponse.countries).hasSize(5)
            val croatia = countriesResponse.countries.find { it -> it.id == croatiaId }
            verifyCroatiaCountryResponse(croatia)
        }
    }

    @Test
    fun mustBeAbleToGetACountryById() {
        suppose("Country exists in database") {
            // 5 base countries are stored in database
        }

        verify("The system must return country with specified id") {
            val result = mockMvc.perform(get("$pathCountries/$croatiaId"))
                    .andExpect(status().isOk)
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andReturn()

            val countryResponse: CountryResponse = objectMapper.readValue(result.response.contentAsString)
            verifyCroatiaCountryResponse(countryResponse)
        }
    }

    @Test
    fun mustReturnNotFoundForInvalidCountryId() {
        verify("The controller returns not found for missing country id") {
            val nonExistingCountryId = 999
            mockMvc.perform(get("$pathCountries/$nonExistingCountryId"))
                    .andExpect(status().isNotFound)
        }
    }

    private fun verifyCroatiaCountryResponse(countryResponse: CountryResponse?) {
        assertThat(countryResponse).isNotNull
        assertThat(countryResponse!!.id).isEqualTo(croatiaId)
        assertThat(countryResponse.iso).isEqualTo("HR")
        assertThat(countryResponse.name).isEqualTo("CROATIA")
        assertThat(countryResponse.nicename).isEqualTo("Croatia")
        assertThat(countryResponse.iso3).isEqualTo("HRV")
        assertThat(countryResponse.numcode).isEqualTo(191)
        assertThat(countryResponse.phonecode).isEqualTo(385)
    }
}
