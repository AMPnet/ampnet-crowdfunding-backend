package com.ampnet.crowdfundingbackend.controller.pojo.response

import com.ampnet.crowdfundingbackend.persistence.model.OrganizationMembership
import java.time.ZonedDateTime

data class OrganizationMembershipsResponse(val members: List<OrganizationMembershipResponse>)

data class OrganizationMembershipResponse(
    val uuid: String,
    val name: String,
    val role: String,
    val memberSince: ZonedDateTime
) {
    constructor(membership: OrganizationMembership, name: String) : this(
        membership.userUuid.toString(), name, membership.role.name, membership.createdAt
    )
}
