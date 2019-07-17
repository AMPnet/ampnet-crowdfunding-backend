package com.ampnet.crowdfundingbackend.userservice

import com.ampnet.crowdfundingbackend.userservice.pojo.UserResponse
import java.util.UUID

interface UserService {
    fun getUsers(uuids: List<UUID>): List<UserResponse>
}
