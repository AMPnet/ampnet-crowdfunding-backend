package com.ampnet.crowdfundingbackend.controller.pojo.request

import com.ampnet.crowdfundingbackend.enums.Currency
import com.ampnet.crowdfundingbackend.validation.InvestmentConstraint
import java.math.BigDecimal
import java.time.ZonedDateTime
import javax.validation.constraints.Size

data class ProjectRequest(

    val organizationId: Int,

    @Size(max = 255)
    val name: String,

    @Size(max = 255)
    val description: String,

    @Size(max = 128)
    val location: String,

    @Size(max = 255)
    val locationText: String,

    @Size(max = 16)
    val returnToInvestment: String,

    val startDate: ZonedDateTime,

    val endDate: ZonedDateTime,

    @InvestmentConstraint
    val expectedFunding: BigDecimal,

    val currency: Currency,

    @InvestmentConstraint
    val minPerUser: BigDecimal,

    @InvestmentConstraint
    val maxPerUser: BigDecimal,

    val active: Boolean
)
