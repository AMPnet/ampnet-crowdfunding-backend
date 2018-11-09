package com.ampnet.crowdfundingbackend.service.pojo

import com.ampnet.crowdfundingbackend.controller.pojo.request.OrganizationRequest
import com.ampnet.crowdfundingbackend.persistence.model.User

data class OrganizationServiceRequest(
    val name: String,
    val legalInfo: String,
    val owner: User,
    val documentHashes: List<String>
) {
    constructor(request: OrganizationRequest, user: User, documentHashes: List<String>): this(
            request.name,
            request.legalInfo,
            user,
            documentHashes
    )
}
