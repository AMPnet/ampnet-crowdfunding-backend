package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.service.pojo.CreateUserServiceRequest
import java.util.Optional

interface UserService {
    fun create(request: CreateUserServiceRequest): User
    fun findAll(): List<User>
    fun delete(id: Int)
    fun find(username: String): Optional<User>
    fun find(id: Int): Optional<User>
}
