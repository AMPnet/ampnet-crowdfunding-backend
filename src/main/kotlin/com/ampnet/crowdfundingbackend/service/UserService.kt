package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.controller.pojo.request.UserUpdateRequest
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.service.pojo.CreateUserServiceRequest

interface UserService {
    fun create(request: CreateUserServiceRequest): User
    fun update(request: UserUpdateRequest): User
    fun findAll(): List<User>
    fun delete(id: Int)
    fun find(username: String): User?
    fun find(id: Int): User?
}
