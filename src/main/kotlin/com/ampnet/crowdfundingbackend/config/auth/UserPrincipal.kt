package com.ampnet.crowdfundingbackend.config.auth

import com.ampnet.crowdfundingbackend.persistence.model.User

data class UserPrincipal(
    val email: String,
    val authorities: Set<String>,
    val enabled: Boolean
) {
    constructor(user: User): this(
        user.email,
        user.getAuthorities().asSequence().map { it.authority }.toSet(),
        user.enabled
    )
}
