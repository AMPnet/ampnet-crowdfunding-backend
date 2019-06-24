package com.ampnet.crowdfundingbackend.service.pojo

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

    @PhoneNumberConstraint
    val phoneNumber: String?,

    val authMethod: AuthMethod
)
