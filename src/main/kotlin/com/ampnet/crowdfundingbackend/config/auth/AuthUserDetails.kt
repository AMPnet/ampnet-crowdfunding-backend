package com.ampnet.crowdfundingbackend.config.auth

import com.ampnet.crowdfundingbackend.persistence.model.User
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class AuthUserDetails(
    private val email: String,
    private val userAuthorities: Collection<GrantedAuthority>,
    private val enabled: Boolean
) : UserDetails {

    constructor(user: User): this(user.email, user.getAuthorities(), user.enabled)

    private val hidden = "Hidden"

    override fun getAuthorities(): Collection<GrantedAuthority> {
        return userAuthorities
    }

    override fun isEnabled(): Boolean {
        return enabled
    }

    override fun getUsername(): String {
        return email
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
