package com.ampnet.crowdfundingbackend.config

import com.ampnet.crowdfundingbackend.persistence.model.AuthMethod
import com.ampnet.crowdfundingbackend.service.UserService
import mu.KLogging
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
) : AuthenticationProvider {

    companion object : KLogging()

    // TODO: Analyze security architecture (jwt, custom auth provider, etc)
    override fun authenticate(authentication: Authentication): Authentication {
        val email = authentication.name
        val user = userService.find(email)

        if (!user.isPresent) {
            logger.info { "User with email: $email not present in database." }
            throw BadCredentialsException("1000")
        }

        val userRights = userService.getAuthority(user.get())
        logger.debug { "User has authorities: ${userRights.joinToString(", ")}" }

        val authToken = when (user.get().authMethod) {
            AuthMethod.EMAIL -> {
                val storedPasswordHash = user.get().password
                val providedPassword = authentication.credentials.toString()
                if (!passwordEncoder.matches(providedPassword, storedPasswordHash)) {
                    logger.info { "User passwords do not match" }
                    throw BadCredentialsException("Wrong password!")
                }
                UsernamePasswordAuthenticationToken(email, providedPassword, userRights)
            }
            AuthMethod.FACEBOOK, AuthMethod.GOOGLE -> {
                // TODO: rethink about credentials null
                UsernamePasswordAuthenticationToken(email, null, userRights)
            }
        }

        logger.debug { "User is authenticate using ${user.get().authMethod} method." }
        return authToken
    }

    override fun supports(authentication: Class<*>): Boolean {
        return authentication == UsernamePasswordAuthenticationToken::class.java
    }
}