package com.ampnet.crowdfundingbackend.service.pojo

import com.ampnet.crowdfundingbackend.controller.pojo.request.SignupRequestUserInfo
import com.ampnet.crowdfundingbackend.validation.CountryConstraint
import com.ampnet.crowdfundingbackend.validation.PasswordConstraint
import com.ampnet.crowdfundingbackend.enums.AuthMethod
import com.ampnet.crowdfundingbackend.validation.EmailConstraint
import com.ampnet.crowdfundingbackend.validation.NameConstraint
import com.ampnet.crowdfundingbackend.validation.PhoneNumberConstraint
import javax.validation.constraints.NotNull

data class CreateUserServiceRequest(

    @EmailConstraint
    @NotNull
    val email: String,

    @PasswordConstraint
    val password: String?,

    @NameConstraint
    val firstName: String?,

    @NameConstraint
    val lastName: String?,

    @CountryConstraint
    val countryId: Int?,

    @PhoneNumberConstraint
    val phoneNumber: String?,

    val authMethod: AuthMethod
) {

    constructor(signupRequestUserInfo: SignupRequestUserInfo, signupMethod: AuthMethod) : this(
            signupRequestUserInfo.email,
            signupRequestUserInfo.password,
            signupRequestUserInfo.firstName,
            signupRequestUserInfo.lastName,
            signupRequestUserInfo.countryId,
            signupRequestUserInfo.phoneNumber,
            signupMethod
    )

    constructor(socialUserInfo: SocialUser, signupMethod: AuthMethod) : this(
            socialUserInfo.email,
            null,
            socialUserInfo.firstName,
            socialUserInfo.lastName,
            socialUserInfo.countryId,
            null,
            signupMethod
    )
}
