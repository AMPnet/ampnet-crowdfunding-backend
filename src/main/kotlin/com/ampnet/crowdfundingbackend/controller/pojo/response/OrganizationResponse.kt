package com.ampnet.crowdfundingbackend.controller.pojo.response

import com.ampnet.crowdfundingbackend.persistence.model.Organization
import java.time.ZonedDateTime

data class OrganizationResponse(
    val id: Int,
    val name: String,
    val createdAt: ZonedDateTime,
    val approved: Boolean,
    val legalInfo: String,
    val walletHash: String?
) {
    constructor(organization: Organization) : this(
        organization.id,
        organization.name,
        organization.createdAt,
        organization.approved,
        organization.legalInfo.orEmpty(),
        organization.wallet?.hash
    )
}

data class OrganizationListResponse(val organizations: List<OrganizationResponse>)

data class OrganizationWithDocumentResponse(
    val id: Int,
    val name: String,
    val createdAt: ZonedDateTime,
    val approved: Boolean,
    val legalInfo: String,
    val documents: List<DocumentResponse>,
    val walletHash: String?
) {
    constructor(organization: Organization) : this(
        organization.id,
        organization.name,
        organization.createdAt,
        organization.approved,
        organization.legalInfo.orEmpty(),
        organization.documents?.map { DocumentResponse(it) }.orEmpty(),
        organization.wallet?.hash
    )
}
