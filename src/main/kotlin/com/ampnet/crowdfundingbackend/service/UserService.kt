package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.controller.pojo.SignupUserRequest
import com.ampnet.crowdfundingbackend.persistence.model.User
import java.util.*

interface UserService {
    fun create(user: SignupUserRequest): User
    fun findAll(): List<User>
    fun delete(id: Long)
    fun find(username: String): Optional<User>
    fun find(id: Long): Optional<User>
}