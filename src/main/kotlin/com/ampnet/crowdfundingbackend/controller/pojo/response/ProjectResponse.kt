package com.ampnet.crowdfundingbackend.controller.pojo.response

import com.ampnet.crowdfundingbackend.enums.Currency
import com.ampnet.crowdfundingbackend.persistence.model.Project
import java.time.ZonedDateTime

data class ProjectResponse(
    val id: Int,
    val name: String,
    val description: String,
    val location: String,
    val locationText: String,
    val returnOnInvestment: String,
    val startDate: ZonedDateTime,
    val endDate: ZonedDateTime,
    val expectedFunding: Long,
    val currency: Currency,
    val minPerUser: Long,
    val maxPerUser: Long,
    val mainImage: String?,
    val gallery: List<String>,
    val news: List<String>,
    val active: Boolean,
    val walletHash: String?
) {
    constructor(project: Project) : this(
        project.id,
        project.name,
        project.description,
        project.location,
        project.locationText,
        project.returnOnInvestment,
        project.startDate,
        project.endDate,
        project.expectedFunding,
        project.currency,
        project.minPerUser,
        project.maxPerUser,
        project.mainImage,
        project.gallery.orEmpty(),
        project.newsLinks.orEmpty(),
        project.active,
        project.wallet?.hash
    )
}

data class ProjectListResponse(val projects: List<ProjectResponse>)
