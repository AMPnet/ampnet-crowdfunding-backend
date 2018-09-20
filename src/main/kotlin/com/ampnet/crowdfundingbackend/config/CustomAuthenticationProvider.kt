package com.ampnet.crowdfundingbackend.config

import com.ampnet.crowdfundingbackend.persistence.model.LoginMethod
import com.ampnet.crowdfundingbackend.service.FacebookService
import com.ampnet.crowdfundingbackend.service.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import javax.annotation.Resource

@Component
class CustomAuthenticationProvider: AuthenticationProvider {

    @Resource(name = "userService")
    lateinit var userService: UserService

    @Autowired
    lateinit var facebookService: FacebookService

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    override fun authenticate(authentication: Authentication): Authentication {
        val email = authentication.name
        val user = userService.find(email)

        if (!user.isPresent) {
            throw BadCredentialsException("1000")
        }

        val userRights = userService.getAuthority(user.get())

        return when (user.get().loginMethod) {
            LoginMethod.REGULAR -> {
                val storedPasswordHash = user.get().password
                val providedPassword = authentication.credentials.toString()
                if (!passwordEncoder.matches(providedPassword, storedPasswordHash)) {
                    throw BadCredentialsException("Wrong password!")
                }
                UsernamePasswordAuthenticationToken(email, providedPassword, userRights)
            }
            LoginMethod.FACEBOOK, LoginMethod.GOOGLE -> {
                UsernamePasswordAuthenticationToken(email, null, userRights)
            }
        }
    }

    override fun supports(authentication: Class<*>): Boolean {
        return authentication == UsernamePasswordAuthenticationToken::class.java
    }

}