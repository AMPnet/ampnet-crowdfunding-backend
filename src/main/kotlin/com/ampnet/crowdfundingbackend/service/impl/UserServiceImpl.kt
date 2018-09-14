package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.controller.pojo.SignupUserRequest
import com.ampnet.crowdfundingbackend.enums.UserRoleType
import com.ampnet.crowdfundingbackend.persistence.model.Role
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.persistence.repository.RoleDao
import com.ampnet.crowdfundingbackend.persistence.repository.UserDao
import com.ampnet.crowdfundingbackend.service.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*

@Service("userService")
class UserServiceImpl(val userDao: UserDao, val roleDao: RoleDao): UserService, UserDetailsService {

    val userRole: Role by lazy {
        roleDao.getOne(UserRoleType.USER.id)
    }

    val adminRole: Role by lazy {
        roleDao.getOne(UserRoleType.ADMIN.id)
    }

    var bcryptEncoder: BCryptPasswordEncoder = BCryptPasswordEncoder(12)

    override fun loadUserByUsername(username: String): UserDetails? {
        val userOptional = userDao.findByUsername(username)

        if (userOptional.isPresent) {
            val user = userOptional.get()
            return org.springframework.security.core.userdetails.User(
                    user.username,
                    user.password,
                    getAuthority(user)
            )
        }
        return null
    }

    fun getAuthority(user: User): Set<SimpleGrantedAuthority> {
        val role = "ROLE_" + user.role.name
        return setOf(SimpleGrantedAuthority(role))
    }

    override fun create(request: SignupUserRequest): User {
        val user = User::class.java.newInstance()
        user.username = request.username
        user.password = bcryptEncoder.encode(request.password)
        user.role = userRole
        user.createdAt = ZonedDateTime.now()
        return userDao.save(user)
    }

    override fun findAll(): List<User> {
        return userDao.findAll()
    }

    override fun delete(id: Long) {
        userDao.deleteById(id)
    }

    override fun find(username: String): Optional<User> {
        return userDao.findByUsername(username)
    }

    override fun find(id: Long): Optional<User> {
        return userDao.findById(id)
    }
}