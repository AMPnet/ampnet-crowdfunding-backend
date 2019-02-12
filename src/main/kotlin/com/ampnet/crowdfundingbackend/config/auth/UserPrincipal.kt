package com.ampnet.crowdfundingbackend.config.auth

import com.ampnet.crowdfundingbackend.persistence.model.User

data class UserPrincipal(
    val email: String,
    val authorities: Set<String>,
    val completeProfile: Boolean,
    val enabled: Boolean
) {
    constructor(user: User) : this(
        user.email,
        user.getAuthorities().asSequence().map { it.authority }.toSet(),
        (user.firstName != null && user.lastName != null && user.country != null && user.phoneNumber != null),
        user.enabled
    )
}
