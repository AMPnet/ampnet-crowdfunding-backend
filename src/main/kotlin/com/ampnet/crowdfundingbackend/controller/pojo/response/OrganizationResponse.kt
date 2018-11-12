package com.ampnet.crowdfundingbackend.controller.pojo.response

import com.ampnet.crowdfundingbackend.persistence.model.Organization
import java.time.ZonedDateTime

data class OrganizationResponse(
    val id: Int,
    val name: String,
    val createdByUser: String,
    val createdAt: ZonedDateTime,
    val approved: Boolean,
    val legalInfo: String,
    val documents: List<String>
) {
    constructor(organization: Organization): this(
            organization.id,
            organization.name,
            organization.createdByUser.getFullName(),
            organization.createdAt,
            organization.approved,
            organization.legalInfo,
            organization.documents
    )
}

data class OrganizationListResponse(val organizations: List<OrganizationResponse>)
