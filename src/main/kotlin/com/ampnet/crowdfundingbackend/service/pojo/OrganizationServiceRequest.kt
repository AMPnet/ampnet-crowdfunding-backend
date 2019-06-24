package com.ampnet.crowdfundingbackend.service.pojo

import com.ampnet.crowdfundingbackend.controller.pojo.request.OrganizationRequest

data class OrganizationServiceRequest(
    val name: String,
    val legalInfo: String,
    val ownerUuid: String
) {
    constructor(request: OrganizationRequest, userUuid: String) : this(
            request.name,
            request.legalInfo,
            userUuid
    )
}
