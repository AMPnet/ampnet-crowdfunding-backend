package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.config.TokenProvider
import com.ampnet.crowdfundingbackend.controller.pojo.request.FacebookLoginRequest
import com.ampnet.crowdfundingbackend.controller.pojo.request.GoogleLoginRequest
import com.ampnet.crowdfundingbackend.controller.pojo.request.LoginRequest
import com.ampnet.crowdfundingbackend.controller.pojo.response.AuthTokenResponse
import com.ampnet.crowdfundingbackend.persistence.model.LoginMethod
import com.ampnet.crowdfundingbackend.service.FacebookService
import com.ampnet.crowdfundingbackend.service.GoogleService
import com.ampnet.crowdfundingbackend.service.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"], maxAge = 3600)
@RestController
class AuthenticationController {

    @Autowired
    private lateinit var authenticationManager: AuthenticationManager

    @Autowired
    private lateinit var jwtTokenUtil: TokenProvider

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var facebookService: FacebookService

    @Autowired
    private lateinit var googleService: GoogleService

    @PostMapping("token/generate")
    fun register(@RequestBody loginRequest: LoginRequest): ResponseEntity<Any> {
        if (!userService.find(loginRequest.email).isPresent) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User not registered.")
        }
        val authentication = authenticationManager.authenticate(UsernamePasswordAuthenticationToken(
            loginRequest.email, loginRequest.password
        ))
        SecurityContextHolder.getContext().authentication = authentication
        val token = jwtTokenUtil.generateToken(authentication)
        return ResponseEntity.ok(AuthTokenResponse(token))
    }

    @PostMapping("token/generate/facebook")
    fun login(@RequestBody loginUserRequest: FacebookLoginRequest): ResponseEntity<Any> {
        val facebookProfile = facebookService.getUserProfile(loginUserRequest.token)
        val user = userService.find(facebookProfile.email)
        if (!user.isPresent) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User not registered.")
        }
        if (user.get().loginMethod != LoginMethod.FACEBOOK) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        val authentication = authenticationManager.authenticate(UsernamePasswordAuthenticationToken(
                user.get().email, null)
        )
        SecurityContextHolder.getContext().authentication = authentication
        val token = jwtTokenUtil.generateToken(authentication)
        return ResponseEntity.ok(AuthTokenResponse(token))
    }

    @PostMapping("token/generate/google")
    fun login(@RequestBody loginUserRequest: GoogleLoginRequest): ResponseEntity<Any> {
        val googleProfile = googleService.getUserProfile(loginUserRequest.token)
        val user = userService.find(googleProfile.email)
        if (!user.isPresent) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User not registered.")
        }
        if (user.get().loginMethod != LoginMethod.GOOGLE) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        val authentication = authenticationManager.authenticate(UsernamePasswordAuthenticationToken(
                user.get().email, null)
        )
        SecurityContextHolder.getContext().authentication = authentication
        val token = jwtTokenUtil.generateToken(authentication)
        return ResponseEntity.ok(AuthTokenResponse(token))
    }

}
