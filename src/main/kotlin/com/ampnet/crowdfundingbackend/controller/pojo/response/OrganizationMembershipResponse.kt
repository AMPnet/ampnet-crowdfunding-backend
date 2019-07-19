package com.ampnet.crowdfundingbackend.controller.pojo.response

import com.ampnet.crowdfundingbackend.persistence.model.OrganizationMembership
import com.ampnet.userservice.proto.UserResponse
import java.time.ZonedDateTime
import java.util.UUID

data class OrganizationMembershipsResponse(val members: List<OrganizationMembershipResponse>)

data class OrganizationMembershipResponse(
    val uuid: UUID,
    val firstName: String,
    val lastName: String,
    val role: String,
    val memberSince: ZonedDateTime
) {
    constructor(membership: OrganizationMembership, userResponse: UserResponse?) : this(
        membership.userUuid,
        userResponse?.firstName.orEmpty(),
        userResponse?.lastName.orEmpty(),
        membership.role.name,
        membership.createdAt
    )
}
