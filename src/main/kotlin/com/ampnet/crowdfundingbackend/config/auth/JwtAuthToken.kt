package com.ampnet.crowdfundingbackend.config.auth

import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.util.Collections

class JwtAuthToken(private val token: String, private val userPrincipal: UserPrincipal? = null) : Authentication {

    override fun getAuthorities(): MutableCollection<out GrantedAuthority>? {
        val auths = userPrincipal?.authorities?.map { SimpleGrantedAuthority(it) }?.toList().orEmpty()
        return Collections.checkedCollection(auths, SimpleGrantedAuthority::class.java)
    }

    override fun setAuthenticated(isAuthenticated: Boolean) {
    }

    override fun getName(): String? {
        return userPrincipal?.uuid.toString()
    }

    override fun getCredentials(): Any {
        return token
    }

    override fun getPrincipal(): Any? {
        return userPrincipal
    }

    override fun isAuthenticated(): Boolean {
        return userPrincipal != null
    }

    override fun getDetails(): Any? {
        return userPrincipal?.email
    }
}
