package com.ampnet.crowdfundingbackend.service.pojo

import com.ampnet.crowdfundingbackend.controller.pojo.request.OrganizationInviteRequest
import com.ampnet.crowdfundingbackend.enums.OrganizationRoleType

data class OrganizationInviteServiceRequest(
    val email: String,
    val roleType: OrganizationRoleType,
    val organizationId: Int,
    val invitedByUserUuid: String
) {
    constructor(request: OrganizationInviteRequest, organizationId: Int, userUuid: String) : this(
        request.email, request.roleType, organizationId, userUuid
    )
}
