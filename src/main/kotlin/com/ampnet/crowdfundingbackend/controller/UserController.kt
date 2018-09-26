package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.request.SignupRequest
import com.ampnet.crowdfundingbackend.controller.pojo.request.SignupRequestSocialInfo
import com.ampnet.crowdfundingbackend.controller.pojo.request.SignupRequestUserInfo
import com.ampnet.crowdfundingbackend.controller.pojo.response.UserResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.UsersResponse
import com.ampnet.crowdfundingbackend.exception.InvalidRequestException
import com.ampnet.crowdfundingbackend.persistence.model.AuthMethod
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.service.SocialService
import com.ampnet.crowdfundingbackend.service.UserService
import com.ampnet.crowdfundingbackend.service.pojo.CreateUserServiceRequest
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import javax.validation.Validator

@CrossOrigin(origins = ["*"], maxAge = 3600)
@RestController
class UserController(val userService: UserService,
                     val socialService: SocialService,
                     val objectMapper: ObjectMapper,
                     val validator: Validator) {

    @PreAuthorize("hasAuthority(T(com.ampnet.crowdfundingbackend.enums.PrivilegeType).PRO_PROFILE)")
    @GetMapping("/me")
    fun me(): ResponseEntity<Any> {
        val user = SecurityContextHolder.getContext().authentication.principal as User
        return ResponseEntity.ok(user.email)
    }

    @PreAuthorize("hasAuthority(T(com.ampnet.crowdfundingbackend.enums.PrivilegeType).PRA_PROFILE)")
    @GetMapping("/users")
    fun getUsers(): ResponseEntity<UsersResponse> {
        val users = userService.findAll().map { UserResponse(it) }
        return ResponseEntity.ok(UsersResponse(users))
    }

    @PreAuthorize("hasAuthority(T(com.ampnet.crowdfundingbackend.enums.PrivilegeType).PRA_PROFILE)")
    @GetMapping("/users/{id}")
    fun getUser(@PathVariable("id") id: Int): ResponseEntity<UserResponse> {
        val user = UserResponse(userService.find(id).get())
        return ResponseEntity.ok(user)
    }

    @PostMapping("/signup")
    fun createUser(@RequestBody request: SignupRequest): ResponseEntity<Any> {
        val createUserRequest = createUserRequest(request)
        validateRequestOrThrow(createUserRequest)
        val user = userService.create(createUserRequest)
        return ResponseEntity.ok(UserResponse(user))
    }

    private fun createUserRequest(request: SignupRequest): CreateUserServiceRequest {
        try {
            return when (request.signupMethod) {
                AuthMethod.EMAIL -> {
                    val jsonString = objectMapper.writeValueAsString(request.userInfo)
                    val userInfo: SignupRequestUserInfo = objectMapper.readValue(jsonString)
                    CreateUserServiceRequest(userInfo, request.signupMethod)
                }
                AuthMethod.GOOGLE -> {
                    val jsonString = objectMapper.writeValueAsString(request.userInfo)
                    val socialInfo: SignupRequestSocialInfo = objectMapper.readValue(jsonString)
                    val userInfo = socialService.getGoogleUserInfo(socialInfo.token)
                    CreateUserServiceRequest(userInfo, request.signupMethod)
                }
                AuthMethod.FACEBOOK -> {
                    val jsonString = objectMapper.writeValueAsString(request.userInfo)
                    val socialInfo: SignupRequestSocialInfo = objectMapper.readValue(jsonString)
                    val userInfo = socialService.getFacebookUserInfo(socialInfo.token)
                    CreateUserServiceRequest(userInfo, request.signupMethod)
                }
            }
        } catch (ex: MissingKotlinParameterException) {
            throw InvalidRequestException("Some fields missing or could not be parsed from JSON request.")
        }
    }

    private fun validateRequestOrThrow(request: CreateUserServiceRequest) {
        val errors = validator.validate(request)
        if (!errors.isEmpty()) {
            throw InvalidRequestException(errors.map { it.message }.joinToString(" "))
        }
    }

}
