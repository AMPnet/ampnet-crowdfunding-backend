package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.controller.pojo.request.FacebookSignupRequest
import com.ampnet.crowdfundingbackend.controller.pojo.request.GoogleSignupRequest
import com.ampnet.crowdfundingbackend.controller.pojo.request.SignupRequest
import com.ampnet.crowdfundingbackend.enums.UserRoleType
import com.ampnet.crowdfundingbackend.exception.FacebookNoAccessException
import com.ampnet.crowdfundingbackend.exception.GoogleNoAccessException
import com.ampnet.crowdfundingbackend.exception.UserAlreadyExistsException
import com.ampnet.crowdfundingbackend.persistence.model.LoginMethod
import com.ampnet.crowdfundingbackend.persistence.model.Role
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.persistence.repository.RoleDao
import com.ampnet.crowdfundingbackend.persistence.repository.UserDao
import com.ampnet.crowdfundingbackend.service.FacebookService
import com.ampnet.crowdfundingbackend.service.GoogleService
import com.ampnet.crowdfundingbackend.service.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.ZonedDateTime
import java.util.*

@Service("userService")
class UserServiceImpl(val userDao: UserDao, val roleDao: RoleDao): UserService {

    val userRole: Role by lazy {
        roleDao.getOne(UserRoleType.USER.id)
    }

    val adminRole: Role by lazy {
        roleDao.getOne(UserRoleType.ADMIN.id)
    }

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    lateinit var facebookService: FacebookService

    @Autowired
    lateinit var googleService: GoogleService

    override fun getAuthority(user: User): Set<SimpleGrantedAuthority> {
        val role = "ROLE_" + user.role.name
        return setOf(SimpleGrantedAuthority(role))
    }

    override fun create(request: SignupRequest): User {

        if (userDao.findByEmail(request.email).isPresent) {
            throw UserAlreadyExistsException()
        }

        val user = User::class.java.newInstance()
        user.email = request.email
        user.password = passwordEncoder.encode(request.password)
        user.role = userRole
        user.createdAt = ZonedDateTime.now()
        user.loginMethod = LoginMethod.REGULAR

        return userDao.save(user)

    }

    override fun create(request: FacebookSignupRequest): User {
        val userProfile = facebookService.getUserProfile(request.token)
        val email = userProfile.email
        if(email == null || email.isBlank()) {
            throw FacebookNoAccessException()
        }

        if(userDao.findByEmail(email).isPresent) {
            throw UserAlreadyExistsException()
        }

        val user = User::class.java.newInstance()
        user.email = email
        user.password = ""
        user.role = userRole
        user.createdAt = ZonedDateTime.now()
        user.loginMethod = LoginMethod.FACEBOOK

        return userDao.save(user)
    }

    override fun create(request: GoogleSignupRequest): User {
        val userProfile = googleService.getUserProfile(request.token)
        val email = userProfile.email
        if(email == null || email.isBlank()) {
            throw GoogleNoAccessException()
        }

        if(userDao.findByEmail(email).isPresent) {
            throw UserAlreadyExistsException()
        }

        val user = User::class.java.newInstance()
        user.email = email
        user.password = ""
        user.role = userRole
        user.createdAt = ZonedDateTime.now()
        user.loginMethod = LoginMethod.GOOGLE

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