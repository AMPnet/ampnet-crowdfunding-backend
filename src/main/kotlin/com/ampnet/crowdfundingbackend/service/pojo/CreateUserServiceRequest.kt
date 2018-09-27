package com.ampnet.crowdfundingbackend.service.pojo

import com.ampnet.crowdfundingbackend.persistence.model.AuthMethod

data class CreateUserServiceRequest(
    val email: String,
    val password: String?,
    val authMethod: AuthMethod
)