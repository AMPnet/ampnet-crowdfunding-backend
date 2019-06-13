package com.ampnet.crowdfundingbackend.controller.pojo.response

import com.ampnet.crowdfundingbackend.enums.OrganizationRoleType
import com.ampnet.crowdfundingbackend.persistence.model.OrganizationInvite

data class OrganizationInviteResponse(
    val organizationId: Int,
    val organizationName: String,
    val role: OrganizationRoleType?
) {
    constructor(invite: OrganizationInvite) : this(
            invite.organizationId,
            invite.organization?.name ?: "Missing value",
            OrganizationRoleType.fromInt(invite.role.id)
    )
}

data class OrganizationInvitesListResponse(val organizationInvites: List<OrganizationInviteResponse>)
