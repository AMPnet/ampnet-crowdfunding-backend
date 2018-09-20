package com.ampnet.crowdfundingbackend.controller.pojo.request

data class SignupRequest(val email: String,
                         val password: String,
                         val age: Int,
                         val salary: Int)