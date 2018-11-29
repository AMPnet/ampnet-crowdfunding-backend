package com.ampnet.crowdfundingbackend.service.pojo

import com.ampnet.crowdfundingbackend.controller.pojo.request.OrganizationInviteRequest
import com.ampnet.crowdfundingbackend.enums.OrganizationRoleType
import com.ampnet.crowdfundingbackend.persistence.model.User

data class OrganizationInviteServiceRequest(
    val email: String,
    val roleType: OrganizationRoleType,
    val organizationId: Int,
    val invitedByUser: User
) {
    constructor(request: OrganizationInviteRequest, organizationId: Int, user: User): this(
        request.email, request.roleType, organizationId, user
    )
}
