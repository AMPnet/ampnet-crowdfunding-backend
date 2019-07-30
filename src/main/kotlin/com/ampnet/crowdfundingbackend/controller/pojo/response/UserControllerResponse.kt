package com.ampnet.crowdfundingbackend.controller.pojo.response

import com.ampnet.userservice.proto.UserResponse
import java.util.UUID

data class UserControllerResponse(
    val uuid: UUID,
    val email: String,
    val firstName: String,
    val lastName: String,
    val enabled: Boolean
) {
    constructor(userResponse: UserResponse) : this(
        UUID.fromString(userResponse.uuid),
        userResponse.email,
        userResponse.firstName,
        userResponse.lastName,
        userResponse.enabled
    )
}
