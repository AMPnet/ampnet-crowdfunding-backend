package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.config.ApplicationProperties
import com.ampnet.crowdfundingbackend.controller.pojo.request.UserUpdateRequest
import com.ampnet.crowdfundingbackend.enums.UserRoleType
import com.ampnet.crowdfundingbackend.exception.InvalidRequestException
import com.ampnet.crowdfundingbackend.exception.ResourceAlreadyExistsException
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.enums.AuthMethod
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.persistence.model.MailToken
import com.ampnet.crowdfundingbackend.persistence.model.Role
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.persistence.repository.CountryRepository
import com.ampnet.crowdfundingbackend.persistence.repository.MailTokenRepository
import com.ampnet.crowdfundingbackend.persistence.repository.RoleRepository
import com.ampnet.crowdfundingbackend.persistence.repository.UserRepository
import com.ampnet.crowdfundingbackend.service.MailService
import com.ampnet.crowdfundingbackend.service.UserService
import com.ampnet.crowdfundingbackend.service.pojo.CreateUserServiceRequest
import mu.KLogging
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

@Service
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val countryRepository: CountryRepository,
    private val mailTokenRepository: MailTokenRepository,
    private val mailService: MailService,
    private val passwordEncoder: PasswordEncoder,
    private val applicationProperties: ApplicationProperties
) : UserService {

    companion object : KLogging()

    private val userRole: Role by lazy {
        roleRepository.getOne(UserRoleType.USER.id)
    }

    private val adminRole: Role by lazy {
        roleRepository.getOne(UserRoleType.ADMIN.id)
    }

    @Transactional
    override fun create(request: CreateUserServiceRequest): User {
        if (userRepository.findByEmail(request.email).isPresent) {
            logger.info { "Trying to create user with email that already exists: ${request.email}" }
            throw ResourceAlreadyExistsException(ErrorCode.REG_USER_EXISTS,
                    "User with email: ${request.email} already exists!")
        }

        val userRequest = createUserFromRequest(request)
        val user = userRepository.save(userRequest)

        if (user.authMethod == AuthMethod.EMAIL && user.enabled.not()) {
            val mailToken = createMailToken(user)
            mailService.sendConfirmationMail(user.email, mailToken.token.toString())
        }

        return user
    }

    @Transactional
    override fun update(request: UserUpdateRequest): User {
        val savedUser = userRepository.findByEmail(request.email).orElseThrow {
            logger.info { "Trying to update user with email ${request.email} which does not exists in db." }
            throw ResourceNotFoundException(ErrorCode.USER_MISSING, "User with email: ${request.email} does not exists")
        }
        val user = updateUserFromRequest(savedUser, request)
        return userRepository.save(user)
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

    @Transactional
    override fun confirmEmail(token: UUID): User? {
        val optionalMailToken = mailTokenRepository.findByToken(token)
        if (!optionalMailToken.isPresent) {
            return null
        }
        val mailToken = optionalMailToken.get()
        if (mailToken.isExpired()) {
            logger.info { "User is trying to confirm mail with expired token: $token" }
            throw InvalidRequestException(ErrorCode.REG_EMAIL_EXPIRED_TOKEN, "The token: $token has expired")
        }
        val user = mailToken.user
        user.enabled = true

        mailTokenRepository.delete(mailToken)
        return userRepository.save(user)
    }

    @Transactional
    override fun resendConfirmationMail(user: User) {
        if (user.authMethod != AuthMethod.EMAIL) {
            return
        }

        mailTokenRepository.findByUserId(user.id).ifPresent {
            mailTokenRepository.delete(it)
        }
        val mailToken = createMailToken(user)
        mailService.sendConfirmationMail(user.email, mailToken.token.toString())
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

        request.countryId?.let { id ->
            user.country = countryRepository.findById(id).orElse(null)
        }
        return user
    }

    private fun updateUserFromRequest(user: User, request: UserUpdateRequest): User {
        user.firstName = request.firstName
        user.lastName = request.lastName
        user.phoneNumber = request.phoneNumber
        user.country = countryRepository.findById(request.countryId).orElseThrow {
            throw ResourceNotFoundException(ErrorCode.COUNTRY_MISSING,
                    "Country with id: ${request.countryId} does not exists")
        }
        return user
    }

    private fun createMailToken(user: User): MailToken {
        val mailToken = MailToken::class.java.getConstructor().newInstance()
        mailToken.user = user
        mailToken.token = generateToken()
        mailToken.createdAt = ZonedDateTime.now()
        return mailTokenRepository.save(mailToken)
    }

    private fun generateToken(): UUID = UUID.randomUUID()
}
