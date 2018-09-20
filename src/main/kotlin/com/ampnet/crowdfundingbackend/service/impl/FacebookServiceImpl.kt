package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.service.FacebookService
import org.springframework.social.facebook.api.User
import org.springframework.social.facebook.api.impl.FacebookTemplate
import org.springframework.stereotype.Service

@Service("facebookService")
class FacebookServiceImpl: FacebookService {

    override fun getUserProfile(token: String): User {
        val facebook = FacebookTemplate(token)
        val userProfile = facebook.fetchObject(
                "me",
                org.springframework.social.facebook.api.User::class.java,
                "id", "email", "first_name", "last_name", "location"
        )
        return userProfile
    }

}