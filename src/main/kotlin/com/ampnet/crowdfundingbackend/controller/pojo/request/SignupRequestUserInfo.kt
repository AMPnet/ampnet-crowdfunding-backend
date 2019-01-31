package com.ampnet.crowdfundingbackend.controller.pojo.request

data class SignupRequestUserInfo(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val countryId: Int,
    val phoneNumber: String
) {
    override fun toString(): String {
        return "email: $email, firstName: $firstName, lastName: $lastName, " +
                "countryId: $countryId, phoneNumber: $phoneNumber"
    }
}
