package com.ampnet.crowdfundingbackend.controller.pojo.request

import com.ampnet.crowdfundingbackend.persistence.constraint.ValidCountry
import javax.validation.constraints.Email
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

data class UserUpdateRequest(

    @field:Email(message = "Invalid email format.")
    val email: String,

    @field:Size(min = 1, max = 30)
    val firstName: String,

    @field:Size(min = 1, max = 30)
    val lastName: String,

    @field:ValidCountry(message = "Provided country does not exist.")
    val countryId: Int,

    @field:Pattern(regexp = "(^$|[0-9]{8,12})", message = "Phone number must consist of 8-12 digits.")
    val phoneNumber: String
)
