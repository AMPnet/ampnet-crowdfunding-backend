package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.request.SignupRequest
import com.ampnet.crowdfundingbackend.controller.pojo.request.SignupRequestSocialInfo
import com.ampnet.crowdfundingbackend.controller.pojo.request.SignupRequestUserInfo
import com.ampnet.crowdfundingbackend.controller.pojo.response.UserResponse
import com.ampnet.crowdfundingbackend.exception.InvalidRequestException
import com.ampnet.crowdfundingbackend.persistence.model.AuthMethod
import com.ampnet.crowdfundingbackend.service.SocialService
import com.ampnet.crowdfundingbackend.service.UserService
import com.ampnet.crowdfundingbackend.service.pojo.CreateUserServiceRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import javax.validation.Validator

@RestController
class RegistrationController(
    private val userService: UserService,
    private val socialService: SocialService,
    private val objectMapper: ObjectMapper,
    private val validator: Validator
) {
    companion object : KLogging()

    @PostMapping("/signup")
    fun createUser(@RequestBody request: SignupRequest): ResponseEntity<UserResponse> {
        logger.debug { "Received request to sign up: $request" }
        val createUserRequest = createUserRequest(request)
        validateRequestOrThrow(createUserRequest)
        val user = userService.create(createUserRequest)
        return ResponseEntity.ok(UserResponse(user))
    }

    @GetMapping("/mail-confirmation")
    fun mailConfirmation(@RequestParam("token") token: String): ResponseEntity<Any> {
        logger.debug { "Received to confirm mail with token: $token" }
        try {
            val tokenUuid = UUID.fromString(token)
            userService.emailConfirmation(tokenUuid)?.let {
                logger.info { "Confirmed email for user: ${it.email}" }
                return ResponseEntity.ok().build()
            }
            logger.info { "User trying to confirm mail with non existing token: $tokenUuid" }
            return ResponseEntity.notFound().build()
        } catch (ex: IllegalArgumentException) {
            logger.warn { "User is send token which is not UUID: $token" }
            return ResponseEntity.badRequest().build()
        }
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
            UserController.logger.info("Could not parse SignupRequest: $request", ex)
            throw InvalidRequestException("Some fields missing or could not be parsed from JSON request.", ex)
        }
    }

    private fun validateRequestOrThrow(request: CreateUserServiceRequest) {
        val errors = validator.validate(request)
        if (!errors.isEmpty()) {
            UserController.logger.info { "Invalid CreateUserServiceRequest: $request" }
            throw InvalidRequestException(errors.joinToString(" ") { it.message })
        }
    }
}