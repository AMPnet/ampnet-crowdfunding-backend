package com.ampnet.crowdfundingbackend.service.pojo

import com.ampnet.crowdfundingbackend.enums.Currency
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.User
import java.math.BigDecimal
import java.time.ZonedDateTime

data class CreateProjectServiceRequest(
    val organization: Organization,
    val name: String,
    val description: String,
    val location: String,
    val locationText: String,
    val returnToInvestment: String,
    val startDate: ZonedDateTime,
    val endDate: ZonedDateTime,
    val expectedFunding: BigDecimal,
    val currency: Currency,
    val minPerUser: BigDecimal,
    val maxPerUser: BigDecimal,
    val createdBy: User
)
