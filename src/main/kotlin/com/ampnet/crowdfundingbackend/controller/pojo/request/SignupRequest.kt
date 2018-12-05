package com.ampnet.crowdfundingbackend.controller.pojo.request

import com.ampnet.crowdfundingbackend.enums.AuthMethod

data class SignupRequest(
    val signupMethod: AuthMethod,
    val userInfo: Map<String, String>
)
