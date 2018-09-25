package com.ampnet.crowdfundingbackend.controller.pojo.response

import com.ampnet.crowdfundingbackend.persistence.model.User

data class UserResponse(
    val email: String,
    val role: String
) {
    constructor(user: User) : this(
            user.email,
            user.role.name
    )
}