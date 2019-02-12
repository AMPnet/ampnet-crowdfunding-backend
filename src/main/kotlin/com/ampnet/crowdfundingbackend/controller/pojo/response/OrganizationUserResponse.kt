package com.ampnet.crowdfundingbackend.controller.pojo.response

import com.ampnet.crowdfundingbackend.enums.OrganizationRoleType
import com.ampnet.crowdfundingbackend.persistence.model.User

data class OrganizationUserResponse(
    val name: String,
    val email: String,
    val role: OrganizationRoleType?
    // TODO: set invitation accepted
) {
    constructor(user: User) : this(
            user.getFullName(),
            user.email,
            user.organizations?.first()?.role?.let {
                OrganizationRoleType.fromInt(it.id)
            }
    )
}

data class OrganizationUsersListResponse(val users: List<OrganizationUserResponse>)
