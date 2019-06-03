package com.ampnet.crowdfundingbackend.controller.pojo.request

import com.ampnet.crowdfundingbackend.validation.EmailConstraint
import com.ampnet.crowdfundingbackend.validation.NameConstraint
import com.ampnet.crowdfundingbackend.validation.PhoneNumberConstraint
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

    @PhoneNumberConstraint
    @NotNull
    val phoneNumber: String
)
