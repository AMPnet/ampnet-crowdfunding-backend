package com.ampnet.crowdfundingbackend.config.auth

import com.ampnet.crowdfundingbackend.exception.TokenException
import mu.KLogging
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Component

@Component
class JwtAuthenticationProvider(val tokenProvider: TokenProvider) : AuthenticationProvider {

    companion object : KLogging()

    override fun authenticate(authentication: Authentication): Authentication {
        val token = authentication.credentials

        if (token is String) {
            try {
                val userPrincipal = tokenProvider.parseToken(token)
                return JwtAuthToken(token, userPrincipal)
            } catch (ex: TokenException) {
                logger.info("Invalid JWT token", ex)
                SecurityContextHolder.clearContext()
                throw BadCredentialsException("Invalid JWT token")
            }
        }
        logger.info { "Missing Authentication credentials" }
        throw UsernameNotFoundException("Authentication is missing JWT token!")
    }

    override fun supports(authentication: Class<*>): Boolean =
            authentication == JwtAuthToken::class.java
}
