package com.ampnet.crowdfundingbackend.config.auth

import mu.KLogging
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Component

@Component
class CustomAuthenticationProvider : AuthenticationProvider {

    companion object : KLogging()

    private val hidden = "Hidden"

    override fun authenticate(authentication: Authentication): Authentication {
        (authentication.principal as? UserPrincipal)?.let {
            val authorities = getAuthorities(it.authorities)
            return UsernamePasswordAuthenticationToken(it, hidden, authorities)
        }
        throw UsernameNotFoundException("Authentication principal is not in UserPrincipal format")
    }

    override fun supports(authentication: Class<*>): Boolean =
            authentication == UsernamePasswordAuthenticationToken::class.java

    private fun getAuthorities(authorities: Set<String>): Set<SimpleGrantedAuthority> =
            authorities.map { SimpleGrantedAuthority(it) }.toSet()

}
