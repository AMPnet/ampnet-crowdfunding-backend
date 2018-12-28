package com.ampnet.crowdfundingbackend.controller.pojo.request

import com.ampnet.crowdfundingbackend.validation.EmailConstraint

data class MailCheckRequest(
    @EmailConstraint
    val email: String
)
