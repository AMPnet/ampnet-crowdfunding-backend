package com.ampnet.crowdfundingbackend.controller.pojo.request

data class SignupRequestUserInfo(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val phoneNumber: String
) {
    override fun toString(): String =
        "email: $email, firstName: $firstName, lastName: $lastName, phoneNumber: $phoneNumber"
}
