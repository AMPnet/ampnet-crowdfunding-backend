package com.ampnet.crowdfundingbackend.config.auth

import com.ampnet.crowdfundingbackend.enums.AuthMethod
import com.ampnet.crowdfundingbackend.service.UserService
import mu.KLogging
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.DisabledException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class CustomAuthenticationProvider(
    private val userService: UserService,
    private val passwordEncoder: PasswordEncoder
) : AuthenticationProvider {

    companion object : KLogging()

    private val hidden = "Hidden"

    // TODO: Analyze security architecture (jwt, custom auth provider, etc)
    override fun authenticate(authentication: Authentication): Authentication {
        val email = authentication.name
        val user = userService.find(email)
        if (user == null) {
            logger.info { "User with email: $email not present in database." }
            throw BadCredentialsException("1000")
        }

        val userRights = user.getAuthorities()
        logger.debug { "User has authorities: ${userRights.joinToString(", ")}" }

        when (user.authMethod) {
            AuthMethod.EMAIL -> {
                val storedPasswordHash = user.password
                val providedPassword = authentication.credentials.toString()
                if (!passwordEncoder.matches(providedPassword, storedPasswordHash)) {
                    logger.info { "User passwords do not match" }
                    throw BadCredentialsException("Wrong password!")
                }
                if (!user.enabled) {
                    throw DisabledException("User Email not confirmed.")
                }
            }
            AuthMethod.FACEBOOK, AuthMethod.GOOGLE -> {
                if (!user.enabled) {
                    throw DisabledException("This account has been disabled!")
                }
            }
        }
        val userPrincipal = UserPrincipal(user)
        val authToken = UsernamePasswordAuthenticationToken(userPrincipal, hidden, userRights)

        logger.debug { "User is authenticate using ${user.authMethod} method." }
        return authToken
    }

    override fun supports(authentication: Class<*>): Boolean {
        return authentication == UsernamePasswordAuthenticationToken::class.java
    }
}
