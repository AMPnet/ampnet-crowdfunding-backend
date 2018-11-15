package com.ampnet.crowdfundingbackend.controller.pojo.response

import com.ampnet.crowdfundingbackend.persistence.model.User

data class OrganizationUserResponse(
    val name: String,
    val email: String,
    val role: String
    // TODO: set invitation accepted
) {
    constructor(user: User): this(user.getFullName(), user.email, user.organizations?.first()?.role?.name ?: "missing")
}

data class OrganizationUsersListResponse(val users: List<OrganizationUserResponse>)
