package com.ampnet.crowdfundingbackend.config.auth

import com.ampnet.crowdfundingbackend.persistence.model.User
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class AuthUserDetails(val user: User): UserDetails {

    private val hidden = "Hidden"

    override fun getAuthorities(): Collection<GrantedAuthority> {
        return user.getAuthorities()
    }

    override fun isEnabled(): Boolean {
        return user.enabled
    }

    override fun getUsername(): String {
        return user.email
    }

    override fun getPassword(): String {
        return hidden
    }

    override fun isCredentialsNonExpired(): Boolean {
        return true
    }

    override fun isAccountNonExpired(): Boolean {
        return true
    }

    override fun isAccountNonLocked(): Boolean {
        return true
    }
}
