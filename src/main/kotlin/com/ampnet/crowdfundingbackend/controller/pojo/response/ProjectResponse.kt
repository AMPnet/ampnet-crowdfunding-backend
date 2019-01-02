package com.ampnet.crowdfundingbackend.controller.pojo.response

import com.ampnet.crowdfundingbackend.enums.Currency
import com.ampnet.crowdfundingbackend.persistence.model.Project
import java.math.BigDecimal
import java.time.ZonedDateTime

data class ProjectResponse(
    val id: Int,
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
    val mainImage: String?,
    val gallery: List<String>,
    val active: Boolean,
    val organization: OrganizationSmallResponse,
    val createByUser: String,
    val currentFunding: BigDecimal
) {
    constructor(project: Project, currentFunding: BigDecimal): this(
            project.id,
            project.name,
            project.description,
            project.location,
            project.locationText,
            project.returnToInvestment,
            project.startDate,
            project.endDate,
            project.expectedFunding,
            project.currency,
            project.minPerUser,
            project.maxPerUser,
            project.mainImage,
            project.gallery.orEmpty(),
            project.active,
            OrganizationSmallResponse(project.organization),
            project.createdBy.getFullName(),
            currentFunding
    )
}
