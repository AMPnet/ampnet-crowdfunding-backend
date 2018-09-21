package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.service.SocialService
import com.ampnet.crowdfundingbackend.service.pojo.SocialUser
import org.springframework.social.facebook.api.impl.FacebookTemplate
import org.springframework.social.google.api.impl.GoogleTemplate
import org.springframework.stereotype.Service

@Service
class SocialServiceImpl: SocialService {

    override fun getFacebookUserInfo(token: String): SocialUser {
        val facebook = FacebookTemplate(token)
        val userProfile = facebook.fetchObject(
                "me",
                org.springframework.social.facebook.api.User::class.java,
                "id", "email", "first_name", "last_name", "location"
        )
        return SocialUser(userProfile.email)
    }

    override fun getGoogleUserInfo(token: String): SocialUser {
        val template = GoogleTemplate(token)
        val userInfo = template.userOperations().userInfo
        return SocialUser(userInfo.email)
    }

}