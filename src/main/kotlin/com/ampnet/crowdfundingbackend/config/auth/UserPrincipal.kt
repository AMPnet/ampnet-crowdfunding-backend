package com.ampnet.crowdfundingbackend.config.auth

import com.ampnet.crowdfundingbackend.persistence.model.User
import org.springframework.security.core.GrantedAuthority

data class UserPrincipal(
    val email: String,
    val userAuthorities: Collection<GrantedAuthority>,
    val enabled: Boolean
) {
    constructor(user: User): this(user.email, user.getAuthorities(), user.enabled)
}
