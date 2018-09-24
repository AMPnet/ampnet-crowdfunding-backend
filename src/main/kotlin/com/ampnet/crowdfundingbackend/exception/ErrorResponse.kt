package com.ampnet.crowdfundingbackend.exception

data class ErrorResponse(val reason: String, val errors: List<String>)
