package com.ampnet.crowdfundingbackend.controller.pojo.request

import com.ampnet.crowdfundingbackend.validation.EmailConstraint
import com.ampnet.crowdfundingbackend.validation.NameConstraint
import com.ampnet.crowdfundingbackend.validation.PhoneNumberConstraint
import com.ampnet.crowdfundingbackend.validation.CountryConstraint
import javax.validation.constraints.NotNull

data class UserUpdateRequest(

    @EmailConstraint
    @NotNull
    val email: String,

    @NameConstraint
    @NotNull
    val firstName: String,

    @NameConstraint
    @NotNull
    val lastName: String,

    @CountryConstraint
    @NotNull
    val countryId: Int,

    @PhoneNumberConstraint
    @NotNull
    val phoneNumber: String
)
