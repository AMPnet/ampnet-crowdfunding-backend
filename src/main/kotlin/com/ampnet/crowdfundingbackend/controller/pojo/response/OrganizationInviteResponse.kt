package com.ampnet.crowdfundingbackend.controller.pojo.response

import com.ampnet.crowdfundingbackend.persistence.model.OrganizationInvite

data class OrganizationInviteResponse(
    val organizationId: Int,
    val organizationName: String,
    val role: String,
    val invitedBy: String
) {
    constructor(invite: OrganizationInvite): this(
            invite.organizationId,
            invite.organization!!.name,
            invite.role.name,
            invite.invitedByUser!!.getFullName()
    )
}

data class OrganizationInvitesListResponse(val organizationInvites: List<OrganizationInviteResponse>)
