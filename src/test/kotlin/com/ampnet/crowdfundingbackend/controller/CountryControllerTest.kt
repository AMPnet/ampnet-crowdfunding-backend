package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.TestBase
import com.ampnet.crowdfundingbackend.controller.pojo.response.CountriesListResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.CountryResponse
import com.ampnet.crowdfundingbackend.enums.UserRoleType
import com.ampnet.crowdfundingbackend.security.WithMockCrowdfoundUser
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

class CountryControllerTest : TestBase() {

    private val pathCountries = "/countries"
    private val croatiaId = 3

    @Test
    @WithMockCrowdfoundUser(role = UserRoleType.USER)
    fun mustBeAbleToGetAListOfAllCountries() {
        suppose("Countries exists in database") {
            // 5 base countries are stored in database
        }

        verify("The system returns a list of countries") {
            val result = mockMvc.perform(get(pathCountries))
                    .andExpect(MockMvcResultMatchers.status().isOk)
                    .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andReturn()

            val countriesResponse: CountriesListResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(countriesResponse.countries).hasSize(5)
            val croatia = countriesResponse.countries.find { it -> it.id == croatiaId }
            verifyCroatiaCountryResponse(croatia)
        }
    }

    @Test
    @WithMockCrowdfoundUser(role = UserRoleType.ADMIN)
    fun mustBeAbleToGetACountryById() {
        suppose("Country exists in database") {
            // 5 base countries are stored in database
        }

        verify("The system must return country with specified id") {
            val result = mockMvc.perform(get("$pathCountries/$croatiaId"))
                    .andExpect(MockMvcResultMatchers.status().isOk)
                    .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andReturn()

            val countryResponse: CountryResponse = objectMapper.readValue(result.response.contentAsString)
            verifyCroatiaCountryResponse(countryResponse)
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
