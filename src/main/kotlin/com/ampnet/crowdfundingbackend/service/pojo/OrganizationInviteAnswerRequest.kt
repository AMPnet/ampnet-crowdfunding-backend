package com.ampnet.crowdfundingbackend.service.pojo

import java.util.UUID

data class OrganizationInviteAnswerRequest(
    val userUuid: UUID,
    val email: String,
    val join: Boolean,
    val organizationId: Int
)
