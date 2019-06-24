package com.ampnet.crowdfundingbackend.service.pojo

data class OrganizationInviteAnswerRequest(
    val userUuid: String,
    val email: String,
    val join: Boolean,
    val organizationId: Int
)
