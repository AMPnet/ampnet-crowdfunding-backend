package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.service.pojo.CreateUserServiceRequest
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.util.*

interface UserService {
    fun create(request: CreateUserServiceRequest): User
    fun findAll(): List<User>
    fun delete(id: Int)
    fun find(username: String): Optional<User>
    fun find(id: Int): Optional<User>
    fun getAuthority(user: User): Set<SimpleGrantedAuthority>
}