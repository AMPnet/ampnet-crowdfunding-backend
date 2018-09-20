package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.controller.pojo.request.FacebookSignupRequest
import com.ampnet.crowdfundingbackend.controller.pojo.request.GoogleSignupRequest
import com.ampnet.crowdfundingbackend.controller.pojo.request.SignupRequest
import com.ampnet.crowdfundingbackend.persistence.model.User
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.util.*

interface UserService {
    fun create(request: SignupRequest): User
    fun create(request: FacebookSignupRequest): User
    fun create(request: GoogleSignupRequest): User
    fun findAll(): List<User>
    fun delete(id: Int)
    fun find(username: String): Optional<User>
    fun find(id: Int): Optional<User>
    fun getAuthority(user: User): Set<SimpleGrantedAuthority>
}