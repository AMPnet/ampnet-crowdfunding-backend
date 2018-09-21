package com.ampnet.crowdfundingbackend.config

import com.ampnet.crowdfundingbackend.persistence.model.AuthMethod
import com.ampnet.crowdfundingbackend.service.UserService
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class CustomAuthenticationProvider(
        val userService: UserService,
        val passwordEncoder: PasswordEncoder
): AuthenticationProvider {

    // TODO: Analyze security architecture (jwt, custom auth provider, etc)
    override fun authenticate(authentication: Authentication): Authentication {
        val email = authentication.name
        val user = userService.find(email)

        if (!user.isPresent) {
            throw BadCredentialsException("1000")
        }

        val userRights = userService.getAuthority(user.get())

        return when (user.get().authMethod) {
            AuthMethod.EMAIL -> {
                val storedPasswordHash = user.get().password
                val providedPassword = authentication.credentials.toString()
                if (!passwordEncoder.matches(providedPassword, storedPasswordHash)) {
                    throw BadCredentialsException("Wrong password!")
                }
                UsernamePasswordAuthenticationToken(email, providedPassword, userRights)
            }
            AuthMethod.FACEBOOK, AuthMethod.GOOGLE -> {
                UsernamePasswordAuthenticationToken(email, null, userRights)
            }
        }
    }

    override fun supports(authentication: Class<*>): Boolean {
        return authentication == UsernamePasswordAuthenticationToken::class.java
    }

}