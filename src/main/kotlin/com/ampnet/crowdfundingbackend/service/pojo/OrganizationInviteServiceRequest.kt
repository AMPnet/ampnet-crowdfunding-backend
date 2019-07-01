package com.ampnet.crowdfundingbackend.service.pojo

import com.ampnet.crowdfundingbackend.controller.pojo.request.OrganizationInviteRequest
import com.ampnet.crowdfundingbackend.enums.OrganizationRoleType
import java.util.UUID

data class OrganizationInviteServiceRequest(
    val email: String,
    val roleType: OrganizationRoleType,
    val organizationId: Int,
    val invitedByUserUuid: UUID
) {
    constructor(request: OrganizationInviteRequest, organizationId: Int, userUuid: UUID) : this(
        request.email, request.roleType, organizationId, userUuid
    )
}
