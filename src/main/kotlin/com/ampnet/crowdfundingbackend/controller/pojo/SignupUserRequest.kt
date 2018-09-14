package com.ampnet.crowdfundingbackend.controller.pojo

data class SignupUserRequest(val username: String,
                             val password: String,
                             val age: Int,
                             val salary: Int)