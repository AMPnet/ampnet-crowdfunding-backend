package com.ampnet.crowdfundingbackend.userservice.pojo

import com.ampnet.userservice.proto.UserResponse
import java.util.UUID

data class UserResponse(
    val uuid: UUID,
    val email: String,
    val firstName: String,
    val lastName: String,
    val enabled: Boolean
) {
    constructor(user: UserResponse) : this(
        UUID.fromString(user.uuid),
        user.email,
        user.firstName,
        user.lastName,
        user.enabled
    )
}
