package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.controller.pojo.request.UserUpdateRequest
import com.ampnet.crowdfundingbackend.enums.UserRoleType
import com.ampnet.crowdfundingbackend.exception.ResourceAlreadyExistsException
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.persistence.model.Role
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.persistence.repository.CountryDao
import com.ampnet.crowdfundingbackend.persistence.repository.RoleDao
import com.ampnet.crowdfundingbackend.persistence.repository.UserDao
import com.ampnet.crowdfundingbackend.service.UserService
import com.ampnet.crowdfundingbackend.service.pojo.CreateUserServiceRequest
import mu.KLogging
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Service
class UserServiceImpl(
    val userDao: UserDao,
    val roleDao: RoleDao,
    val countryDao: CountryDao,
    val passwordEncoder: PasswordEncoder
) : UserService {

    companion object : KLogging()

    val userRole: Role by lazy {
        roleDao.getOne(UserRoleType.USER.id)
    }

    val adminRole: Role by lazy {
        roleDao.getOne(UserRoleType.ADMIN.id)
    }

    @Transactional
    override fun create(request: CreateUserServiceRequest): User {
        if (userDao.findByEmail(request.email).isPresent) {
            logger.info { "Trying to create user with email that already exists: ${request.email}" }
            throw ResourceAlreadyExistsException("User with email: ${request.email} already exists!")
        }

        val user = createUserFromRequest(request)
        return userDao.save(user)
    }

    @Transactional
    override fun update(request: UserUpdateRequest): User {
        val savedUser = userDao.findByEmail(request.email).orElseThrow {
            logger.info { "Trying to update user with email ${request.email} which does not exists in db." }
            throw ResourceNotFoundException("User with email: ${request.email} does not exists")
        }
        val user = updateUserFromRequest(savedUser, request)
        return userDao.save(user)
    }

    @Transactional(readOnly = true)
    override fun findAll(): List<User> {
        return userDao.findAll()
    }

    @Transactional(readOnly = true)
    override fun find(username: String): User? {
        return ServiceUtils.wrapOptional(userDao.findByEmail(username))
    }

    @Transactional(readOnly = true)
    override fun find(id: Int): User? {
        return ServiceUtils.wrapOptional(userDao.findById(id))
    }

    @Transactional
    override fun delete(id: Int) {
        userDao.deleteById(id)
    }

    private fun createUserFromRequest(request: CreateUserServiceRequest): User {
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
        return user
    }

    private fun updateUserFromRequest(user: User, request: UserUpdateRequest): User {
        user.email = request.email
        user.firstName = request.firstName
        user.lastName = request.lastName
        user.phoneNumber = request.phoneNumber
        user.country = countryDao.findById(request.countryId).orElseThrow {
            throw ResourceNotFoundException("Country with id: ${request.countryId} does not exists")
        }
        return user
    }
}
