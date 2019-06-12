package com.ampnet.crowdfundingbackend.config.auth

data class UserPrincipal(
    val email: String,
    val authorities: Set<String>,
    val completeProfile: Boolean,
    val enabled: Boolean
)
