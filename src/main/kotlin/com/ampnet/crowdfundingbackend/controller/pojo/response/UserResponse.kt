package com.ampnet.crowdfundingbackend.controller.pojo.response

import com.ampnet.crowdfundingbackend.persistence.model.User

data class UserResponse(
    val id: Int,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val country: String?,
    val phoneNumber: String?,
    val role: String
) {

    constructor(user: User) : this(
            user.id,
            user.email,
            user.firstName,
            user.lastName,
            user.country?.nicename,
            user.phoneNumber,
            user.role.name
    )
}

data class UsersListResponse(val users: List<UserResponse>)
