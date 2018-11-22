package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.config.auth.UserPrincipal
import com.ampnet.crowdfundingbackend.controller.pojo.request.UserUpdateRequest
import com.ampnet.crowdfundingbackend.controller.pojo.response.UserResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.UsersListResponse
import com.ampnet.crowdfundingbackend.service.UserService
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
import javax.validation.Valid

@CrossOrigin(origins = ["*"], maxAge = 3600)
@RestController
class UserController(private val userService: UserService) {

    companion object : KLogging()

    @GetMapping("/me")
    @PreAuthorize("hasAuthority(T(com.ampnet.crowdfundingbackend.enums.PrivilegeType).PRO_PROFILE)")
    fun me(): ResponseEntity<UserResponse> {
        logger.debug { "Received request for my profile" }
        val userPrincipal = SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        userService.find(userPrincipal.email)?.let {
            return ResponseEntity.ok(UserResponse(it))
        }

        logger.error("Non existing user: ${userPrincipal.email} trying to get his profile")
        return ResponseEntity.notFound().build()
    }

    @PostMapping("/me")
    @PreAuthorize("hasAuthority(T(com.ampnet.crowdfundingbackend.enums.PrivilegeType).PWO_PROFILE)")
    fun updateMyProfile(@RequestBody @Valid request: UserUpdateRequest): ResponseEntity<UserResponse> {
        logger.debug { "User send request to update his profile" }
        val userPrincipal = SecurityContextHolder.getContext().authentication.principal as UserPrincipal
        return if (userPrincipal.email != request.email) {
            logger.info("User trying to update others profile")
            ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        } else {
            val user = userService.update(request)
            ResponseEntity.ok(UserResponse(user))
        }
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority(T(com.ampnet.crowdfundingbackend.enums.PrivilegeType).PRA_PROFILE)")
    fun getUsers(): ResponseEntity<UsersListResponse> {
        logger.debug { "Received request to list all users" }
        val users = userService.findAll().map { UserResponse(it) }
        return ResponseEntity.ok(UsersListResponse(users))
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasAuthority(T(com.ampnet.crowdfundingbackend.enums.PrivilegeType).PRA_PROFILE)")
    fun getUser(@PathVariable("id") id: Int): ResponseEntity<UserResponse> {
        logger.debug { "Received request for user info with id: $id" }
        return userService.find(id)?.let { ResponseEntity.ok(UserResponse(it)) }
                ?: ResponseEntity.notFound().build()
    }
}
