package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.SignupUserRequest
import com.ampnet.crowdfundingbackend.controller.pojo.UserResponse
import com.ampnet.crowdfundingbackend.controller.pojo.UsersResponse
import com.ampnet.crowdfundingbackend.service.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@CrossOrigin(origins = ["*"], maxAge = 3600)
@RestController
class UserController {

    @Autowired
    lateinit var userService: UserService

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    fun getUsers(): ResponseEntity<UsersResponse> {
        val users = userService.findAll().map { UserResponse(it) }
        return ResponseEntity.ok(UsersResponse(users))
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/users/{id}")
    fun getUser(@PathVariable("id") id: Long): ResponseEntity<UserResponse> {
        val user = UserResponse(userService.find(id).get())
        return ResponseEntity.ok(user)
    }

    @PostMapping("/signup")
    fun createUser(@RequestBody @Valid request: SignupUserRequest): ResponseEntity<UserResponse> {
        val user = UserResponse(userService.create(request))
        return ResponseEntity.ok(user)
    }

}
