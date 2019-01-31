package com.ampnet.crowdfundingbackend.controller.pojo.request

import com.ampnet.crowdfundingbackend.enums.Currency
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
    val returnOnInvestment: String,

    val startDate: ZonedDateTime,

    val endDate: ZonedDateTime,

    val expectedFunding: Long,

    val currency: Currency,

    val minPerUser: Long,

    val maxPerUser: Long,

    val active: Boolean
)
