package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.request.SignupRequest
import com.ampnet.crowdfundingbackend.controller.pojo.request.SignupRequestSocialInfo
import com.ampnet.crowdfundingbackend.controller.pojo.request.SignupRequestUserInfo
import com.ampnet.crowdfundingbackend.controller.pojo.response.UserResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.UsersResponse
import com.ampnet.crowdfundingbackend.persistence.model.AuthMethod
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.service.SocialService
import com.ampnet.crowdfundingbackend.service.UserService
import com.ampnet.crowdfundingbackend.service.pojo.CreateUserServiceRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@CrossOrigin(origins = ["*"], maxAge = 3600)
@RestController
class UserController(val userService: UserService,
                     val socialService: SocialService,
                     val objectMapper: ObjectMapper) {

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/me")
    fun me(): ResponseEntity<Any> {
        val user = SecurityContextHolder.getContext().authentication.principal as User
        return ResponseEntity.ok(user.email)
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    fun getUsers(): ResponseEntity<UsersResponse> {
        val users = userService.findAll().map { UserResponse(it) }
        return ResponseEntity.ok(UsersResponse(users))
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/users/{id}")
    fun getUser(@PathVariable("id") id: Int): ResponseEntity<UserResponse> {
        val user = UserResponse(userService.find(id).get())
        return ResponseEntity.ok(user)
    }

    @PostMapping("/signup")
    fun createUser(@RequestBody request: SignupRequest): ResponseEntity<Any> {
        val createUserRequest = when (request.signupMethod) {
            AuthMethod.EMAIL -> {
                val userInfo: SignupRequestUserInfo = objectMapper.convertValue(request.userInfo)
                CreateUserServiceRequest(userInfo.email, userInfo.password, request.signupMethod)
            }
            AuthMethod.GOOGLE -> {
                val socialInfo: SignupRequestSocialInfo = objectMapper.convertValue(request.userInfo)
                val userInfo = socialService.getGoogleUserInfo(socialInfo.token)
                CreateUserServiceRequest(userInfo.email, null, request.signupMethod)
            }
            AuthMethod.FACEBOOK -> {
                val socialInfo: SignupRequestSocialInfo = objectMapper.convertValue(request.userInfo)
                val userInfo = socialService.getFacebookUserInfo(socialInfo.token)
                CreateUserServiceRequest(userInfo.email, null, request.signupMethod)
            }
        }
        val user = userService.create(createUserRequest)
        return ResponseEntity.ok(UserResponse(user))
    }

}
