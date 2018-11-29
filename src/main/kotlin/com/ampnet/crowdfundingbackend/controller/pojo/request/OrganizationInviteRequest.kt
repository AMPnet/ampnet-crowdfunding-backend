package com.ampnet.crowdfundingbackend.controller.pojo.request

import com.ampnet.crowdfundingbackend.enums.OrganizationRoleType

data class OrganizationInviteRequest(
    val email: String,
    val roleType: OrganizationRoleType
)
