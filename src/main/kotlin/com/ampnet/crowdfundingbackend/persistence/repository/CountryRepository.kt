package com.ampnet.crowdfundingbackend.persistence.repository

import com.ampnet.crowdfundingbackend.persistence.model.Country
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface CountryRepository : JpaRepository<Country, Int> {

    fun findByNicename(nicename: String): Optional<Country>
}
