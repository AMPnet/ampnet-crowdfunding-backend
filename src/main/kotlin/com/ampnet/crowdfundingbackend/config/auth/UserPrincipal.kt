package com.ampnet.crowdfundingbackend.config.auth

import java.util.UUID

data class UserPrincipal(
    val uuid: UUID,
    val email: String,
    val name: String,
    val authorities: Set<String>,
    val enabled: Boolean
)
