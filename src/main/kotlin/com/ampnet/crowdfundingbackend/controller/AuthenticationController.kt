package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.config.TokenProvider
import com.ampnet.crowdfundingbackend.controller.pojo.AuthToken
import com.ampnet.crowdfundingbackend.controller.pojo.LoginUserRequest
import com.ampnet.crowdfundingbackend.service.UserService
import org.springframework.beans.factory.annotation.Autowired
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

    @PostMapping("token/generate-token")
    fun register(@RequestBody loginUserRequest: LoginUserRequest): ResponseEntity<AuthToken> {
        val authentication = authenticationManager.authenticate(UsernamePasswordAuthenticationToken(
            loginUserRequest.username, loginUserRequest.password
        ))
        SecurityContextHolder.getContext().authentication = authentication
        val token = jwtTokenUtil.generateToken(authentication)
        return ResponseEntity.ok(AuthToken(token))
    }

}
