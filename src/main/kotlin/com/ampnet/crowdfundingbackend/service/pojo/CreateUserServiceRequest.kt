package com.ampnet.crowdfundingbackend.service.pojo

import com.ampnet.crowdfundingbackend.controller.pojo.request.SignupRequestUserInfo
import com.ampnet.crowdfundingbackend.persistence.constraint.ValidCountry
import com.ampnet.crowdfundingbackend.persistence.constraint.ValidPassword
import com.ampnet.crowdfundingbackend.persistence.model.AuthMethod
import javax.validation.constraints.Email
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

data class CreateUserServiceRequest(

    @field:Email(message = "Invalid email format.")
    val email: String,

    @ValidPassword
    val password: String?,

    @field:Size(min = 1, max = 30)
    val firstName: String?,

    @field:Size(min = 1, max = 30)
    val lastName: String?,

    @field:ValidCountry(message = "Provided country does not exist.")
    val countryId: Int?,

    @field:Pattern(regexp = "(^$|[0-9]{8,12})", message = "Phone number must consist of 8-12 digits.")
    val phoneNumber: String?,

    val authMethod: AuthMethod
) {

    constructor(signupRequestUserInfo: SignupRequestUserInfo, signupMethod: AuthMethod): this(
            signupRequestUserInfo.email,
            signupRequestUserInfo.password,
            signupRequestUserInfo.firstName,
            signupRequestUserInfo.lastName,
            signupRequestUserInfo.countryId,
            signupRequestUserInfo.phoneNumber,
            signupMethod
    )

    constructor(socialUserInfo: SocialUser, signupMethod: AuthMethod): this(
            socialUserInfo.email,
            null,
            socialUserInfo.firstName,
            socialUserInfo.lastName,
            socialUserInfo.countryId,
            null,
            signupMethod
    )
}