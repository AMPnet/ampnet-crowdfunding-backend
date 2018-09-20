package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.request.FacebookSignupRequest
import com.ampnet.crowdfundingbackend.controller.pojo.request.GoogleSignupRequest
import com.ampnet.crowdfundingbackend.controller.pojo.request.SignupRequest
import com.ampnet.crowdfundingbackend.controller.pojo.response.UserResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.UsersResponse
import com.ampnet.crowdfundingbackend.exception.FacebookNoAccessException
import com.ampnet.crowdfundingbackend.exception.GoogleNoAccessException
import com.ampnet.crowdfundingbackend.exception.UserAlreadyExistsException
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.service.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@CrossOrigin(origins = ["*"], maxAge = 3600)
@RestController
class UserController {

    @Autowired
    lateinit var userService: UserService

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
        try {
            val user = UserResponse(userService.create(request))
            return ResponseEntity.ok(user)
        } catch (ex: UserAlreadyExistsException) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("User already exists.")
        }
    }

    @PostMapping("/signup/facebook")
    fun facebookSignUp(@RequestBody request: FacebookSignupRequest): ResponseEntity<Any> {
        try {
            val user = UserResponse(userService.create(request))
            return ResponseEntity.ok(user)
        } catch (ex: FacebookNoAccessException) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Could not fetch user data using FB api.")
        } catch (ex: UserAlreadyExistsException) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("User already exists.")
        }
    }

    @PostMapping("/signup/google")
    fun googleSignUp(@RequestBody request: GoogleSignupRequest): ResponseEntity<Any> {
        try {
            val user = UserResponse(userService.create(request))
            return ResponseEntity.ok(user)
        } catch (ex: GoogleNoAccessException) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Could not fetch user data using Google api.")
        } catch (ex: UserAlreadyExistsException) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("User already exists.")
        }
    }

}
