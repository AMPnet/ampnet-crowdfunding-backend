package com.ampnet.crowdfundingbackend.controller.pojo.request

import com.ampnet.crowdfundingbackend.persistence.model.AuthMethod

data class TokenRequest(val loginMethod: AuthMethod,
                        val credentials: Map<String, String>)