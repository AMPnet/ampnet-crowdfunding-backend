package com.ampnet.crowdfundingbackend.config.auth

data class UserPrincipal(
    val uuid: String,
    val email: String,
    val name: String,
    val authorities: Set<String>,
    val enabled: Boolean
)
