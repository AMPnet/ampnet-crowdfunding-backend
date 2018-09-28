package com.ampnet.crowdfundingbackend.persistence.model

data class FacebookLocation(val id: String, val location: FacebookLocationData)
data class FacebookLocationData(val city: String, val country: String)