package com.ampnet.crowdfundingbackend.controller.pojo

import com.ampnet.crowdfundingbackend.persistence.model.User

data class UserResponse(val username: String,
                        val role: String) {
    constructor(user: User) : this(
            user.username,
            user.role.name
    )
}