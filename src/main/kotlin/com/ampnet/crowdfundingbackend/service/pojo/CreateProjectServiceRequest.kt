package com.ampnet.crowdfundingbackend.service.pojo

import com.ampnet.crowdfundingbackend.controller.pojo.request.ProjectRequest
import com.ampnet.crowdfundingbackend.enums.Currency
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.User
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
    val expectedFunding: Long,
    val currency: Currency,
    val minPerUser: Long,
    val maxPerUser: Long,
    val active: Boolean,
    val createdBy: User
) {
    constructor(request: ProjectRequest, organization: Organization, user: User): this(
        organization,
            request.name,
            request.description,
            request.location,
            request.locationText,
            request.returnToInvestment,
            request.startDate,
            request.endDate,
            request.expectedFunding,
            request.currency,
            request.minPerUser,
            request.maxPerUser,
            request.active,
            user
    )
}
