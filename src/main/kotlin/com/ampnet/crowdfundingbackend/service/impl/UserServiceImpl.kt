package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.enums.UserRoleType
import com.ampnet.crowdfundingbackend.exception.ResourceAlreadyExistsException
import com.ampnet.crowdfundingbackend.persistence.model.Role
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.persistence.repository.CountryDao
import com.ampnet.crowdfundingbackend.persistence.repository.RoleDao
import com.ampnet.crowdfundingbackend.persistence.repository.UserDao
import com.ampnet.crowdfundingbackend.service.UserService
import com.ampnet.crowdfundingbackend.service.pojo.CreateUserServiceRequest
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.ZonedDateTime
import java.util.Optional

@Service
class UserServiceImpl(
    val userDao: UserDao,
    val roleDao: RoleDao,
    val countryDao: CountryDao,
    val passwordEncoder: PasswordEncoder
) : UserService {

    val userRole: Role by lazy {
        roleDao.getOne(UserRoleType.USER.id)
    }

    val adminRole: Role by lazy {
        roleDao.getOne(UserRoleType.ADMIN.id)
    }

    override fun getAuthority(user: User): Set<SimpleGrantedAuthority> {
        val role = "ROLE_" + user.role.name
        return setOf(SimpleGrantedAuthority(role))
    }

    override fun create(request: CreateUserServiceRequest): User {
        if (userDao.findByEmail(request.email).isPresent) {
            throw ResourceAlreadyExistsException("User with email: ${request.email} already exists!")
        }

        val user = User::class.java.newInstance()

        user.email = request.email
        user.password = passwordEncoder.encode(request.password.orEmpty())
        user.firstName = request.firstName
        user.lastName = request.lastName
        user.phoneNumber = request.phoneNumber
        user.role = userRole
        user.createdAt = ZonedDateTime.now()
        user.authMethod = request.authMethod
        user.enabled = true

        request.countryId?.let { id ->
            user.country = countryDao.findById(id).orElse(null)
        }

        return userDao.save(user)
    }

    override fun findAll(): List<User> {
        return userDao.findAll()
    }

    override fun delete(id: Int) {
        userDao.deleteById(id)
    }

    override fun find(username: String): Optional<User> {
        return userDao.findByEmail(username)
    }

    override fun find(id: Int): Optional<User> {
        return userDao.findById(id)
    }
}
