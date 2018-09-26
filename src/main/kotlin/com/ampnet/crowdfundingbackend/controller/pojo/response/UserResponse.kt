package com.ampnet.crowdfundingbackend.controller.pojo.response

import com.ampnet.crowdfundingbackend.persistence.model.User

data class UserResponse(
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val country: String?,
    val phoneNumber: String?,
    val role: String
) {

    constructor(user: User) : this(
            user.email,
            user.firstName,
            user.lastName,
            user.country?.nicename,
            user.phoneNumber,
            user.role.name
    )
}
