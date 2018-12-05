package com.ampnet.crowdfundingbackend.controller.pojo.request

import com.ampnet.crowdfundingbackend.enums.AuthMethod

data class TokenRequest(
    val loginMethod: AuthMethod,
    val credentials: Map<String, String>
)
