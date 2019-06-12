package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.config.ApplicationProperties
import com.ampnet.crowdfundingbackend.enums.UserRoleType
import com.ampnet.crowdfundingbackend.exception.ResourceAlreadyExistsException
import com.ampnet.crowdfundingbackend.enums.AuthMethod
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.persistence.model.Role
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.persistence.repository.RoleRepository
import com.ampnet.crowdfundingbackend.persistence.repository.UserRepository
import com.ampnet.crowdfundingbackend.service.UserService
import com.ampnet.crowdfundingbackend.service.pojo.CreateUserServiceRequest
import mu.KLogging
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Service
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder,
    private val applicationProperties: ApplicationProperties
) : UserService {

    companion object : KLogging()

    private val userRole: Role by lazy {
        roleRepository.getOne(UserRoleType.USER.id)
    }

    @Transactional
    override fun create(request: CreateUserServiceRequest): User {
        if (userRepository.findByEmail(request.email).isPresent) {
            throw ResourceAlreadyExistsException(ErrorCode.REG_USER_EXISTS,
                    "Trying to create user with email that already exists: ${request.email}")
        }

        val userRequest = createUserFromRequest(request)
        val user = userRepository.save(userRequest)

        return user
    }

    @Transactional(readOnly = true)
    override fun findAll(): List<User> {
        return userRepository.findAll()
    }

    @Transactional(readOnly = true)
    override fun find(username: String): User? {
        return ServiceUtils.wrapOptional(userRepository.findByEmail(username))
    }

    @Transactional(readOnly = true)
    override fun find(id: Int): User? {
        return ServiceUtils.wrapOptional(userRepository.findById(id))
    }

    @Transactional(readOnly = true)
    override fun findWithWallet(email: String): User? {
        return ServiceUtils.wrapOptional(userRepository.findByEmailWithWallet(email))
    }

    @Transactional
    override fun delete(id: Int) {
        userRepository.deleteById(id)
    }

    private fun createUserFromRequest(request: CreateUserServiceRequest): User {
        val user = User::class.java.getConstructor().newInstance()
        user.email = request.email
        user.password = passwordEncoder.encode(request.password.orEmpty())
        user.firstName = request.firstName
        user.lastName = request.lastName
        user.phoneNumber = request.phoneNumber
        user.role = userRole
        user.createdAt = ZonedDateTime.now()
        user.authMethod = request.authMethod

        if (user.authMethod == AuthMethod.EMAIL) {
            // user must confirm email if the mail sending service is enabled
            user.enabled = applicationProperties.mail.enabled.not()
        } else {
            // social user is confirmed from social service
            user.enabled = true
        }
        return user
    }
}
