package com.ampnet.crowdfundingbackend.controller.pojo.request

data class SignupRequestUserInfo(val email: String,
                                 val password: String,
                                 val firstName: String,
                                 val lastName: String,
                                 val country: String,
                                 val phoneNumber: String)
