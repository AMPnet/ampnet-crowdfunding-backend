package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.config.auth.AuthUserDetails
import com.ampnet.crowdfundingbackend.controller.pojo.request.SignupRequest
import com.ampnet.crowdfundingbackend.controller.pojo.request.SignupRequestSocialInfo
import com.ampnet.crowdfundingbackend.controller.pojo.request.SignupRequestUserInfo
import com.ampnet.crowdfundingbackend.controller.pojo.response.UserResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.UsersListResponse
import com.ampnet.crowdfundingbackend.exception.InvalidRequestException
import com.ampnet.crowdfundingbackend.persistence.model.AuthMethod
import com.ampnet.crowdfundingbackend.service.SocialService
import com.ampnet.crowdfundingbackend.service.UserService
import com.ampnet.crowdfundingbackend.service.pojo.CreateUserServiceRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import javax.validation.Validator

@CrossOrigin(origins = ["*"], maxAge = 3600)
@RestController
class UserController(
    val userService: UserService,
    val socialService: SocialService,
    val objectMapper: ObjectMapper,
    val validator: Validator
) {

    companion object : KLogging()

    @PreAuthorize("hasAuthority(T(com.ampnet.crowdfundingbackend.enums.PrivilegeType).PRO_PROFILE)")
    @GetMapping("/me")
    fun me(): ResponseEntity<UserResponse> {
        logger.debug { "Received request for my profile" }
        val authUser = SecurityContextHolder.getContext().authentication.principal as AuthUserDetails
        val userOptional = userService.find(authUser.username)
        return if (userOptional.isPresent) {
            ResponseEntity.ok(UserResponse(userOptional.get()))
        } else {
            logger.error("Non existing user: ${authUser.username} trying to get his profile")
            ResponseEntity.notFound().build()
        }
    }

    @PreAuthorize("hasAuthority(T(com.ampnet.crowdfundingbackend.enums.PrivilegeType).PRA_PROFILE)")
    @GetMapping("/users")
    fun getUsers(): ResponseEntity<UsersListResponse> {
        logger.debug { "Received request to list all users" }
        val users = userService.findAll().map { UserResponse(it) }
        return ResponseEntity.ok(UsersListResponse(users))
    }

    @PreAuthorize("hasAuthority(T(com.ampnet.crowdfundingbackend.enums.PrivilegeType).PRA_PROFILE)")
    @GetMapping("/users/{id}")
    fun getUser(@PathVariable("id") id: Int): ResponseEntity<UserResponse> {
        logger.debug { "Received request for user info with id: $id" }
        val optionalUser = userService.find(id)
        return if (optionalUser.isPresent) {
            val user = UserResponse(optionalUser.get())
            ResponseEntity.ok(user)
        } else {
            ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
    }

    @PostMapping("/signup")
    fun createUser(@RequestBody request: SignupRequest): ResponseEntity<Any> {
        logger.debug { "Received request to sign up: $request" }
        val createUserRequest = createUserRequest(request)
        validateRequestOrThrow(createUserRequest)
        val user = userService.create(createUserRequest)
        return ResponseEntity.ok(UserResponse(user))
    }

    private fun createUserRequest(request: SignupRequest): CreateUserServiceRequest {
        try {
            val jsonString = objectMapper.writeValueAsString(request.userInfo)

            return when (request.signupMethod) {
                AuthMethod.EMAIL -> {
                    val userInfo: SignupRequestUserInfo = objectMapper.readValue(jsonString)
                    CreateUserServiceRequest(userInfo, request.signupMethod)
                }
                AuthMethod.GOOGLE -> {
                    val socialInfo: SignupRequestSocialInfo = objectMapper.readValue(jsonString)
                    val userInfo = socialService.getGoogleUserInfo(socialInfo.token)
                    CreateUserServiceRequest(userInfo, request.signupMethod)
                }
                AuthMethod.FACEBOOK -> {
                    val socialInfo: SignupRequestSocialInfo = objectMapper.readValue(jsonString)
                    val userInfo = socialService.getFacebookUserInfo(socialInfo.token)
                    CreateUserServiceRequest(userInfo, request.signupMethod)
                }
            }
        } catch (ex: MissingKotlinParameterException) {
            logger.info("Could not parse SignupRequest: $request", ex)
            throw InvalidRequestException("Some fields missing or could not be parsed from JSON request.", ex)
        }
    }

    private fun validateRequestOrThrow(request: CreateUserServiceRequest) {
        val errors = validator.validate(request)
        if (!errors.isEmpty()) {
            logger.info { "Invalid CreateUserServiceRequest: $request" }
            throw InvalidRequestException(errors.joinToString(" ") { it.message })
        }
    }
}
