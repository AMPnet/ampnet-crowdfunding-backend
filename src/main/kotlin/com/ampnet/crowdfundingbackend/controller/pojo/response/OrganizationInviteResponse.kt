package com.ampnet.crowdfundingbackend.controller.pojo.response

import com.ampnet.crowdfundingbackend.enums.OrganizationRoleType
import com.ampnet.crowdfundingbackend.persistence.model.OrganizationInvite

data class OrganizationInviteResponse(
    val organizationId: Int,
    val organizationName: String,
    val role: OrganizationRoleType,
    val invitedByUser: String
) {
    constructor(invite: OrganizationInvite): this(
            invite.organizationId,
            invite.organization!!.name,
            OrganizationRoleType.fromInt(invite.role.id)!!,
            invite.invitedByUser!!.getFullName()
    )
}

data class OrganizationInvitesListResponse(val organizationInvites: List<OrganizationInviteResponse>)
