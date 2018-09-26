package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.service.SocialService
import com.ampnet.crowdfundingbackend.service.pojo.SocialUser
import org.springframework.social.facebook.api.Page
import org.springframework.social.facebook.api.impl.FacebookTemplate
import org.springframework.social.google.api.impl.GoogleTemplate
import org.springframework.stereotype.Service

@Service
class SocialServiceImpl : SocialService {

    override fun getFacebookUserInfo(token: String): SocialUser {
        val facebook = FacebookTemplate(token)
        val userProfile = facebook.fetchObject(
                "me",
                org.springframework.social.facebook.api.User::class.java,
                "id", "email", "first_name", "last_name", "location"
        )
        var country: String? = null
        if (userProfile.location != null && userProfile.location.id != null) {
            /*
            This block will always throw because in order to use `Page Public Content Access`
            (which we need for location access) app has to be reviewed and approved by Facebook.
            For now, catch and do nothing.
             */
            try { // this block will always throw since facebook app still not reviewed
                val page = facebook.fetchObject(userProfile.location.id, Page::class.java, "location")
                country = page.location.country
            } catch (ex: Exception) { }
        }
        return SocialUser(
                email = userProfile.email,
                firstName = userProfile.firstName,
                lastName = userProfile.lastName,
                country = country
        )
    }

    override fun getGoogleUserInfo(token: String): SocialUser {
        val template = GoogleTemplate(token)
        val userInfo = template.userOperations().userInfo
        return SocialUser(
                email = userInfo.email,
                firstName = userInfo.firstName,
                lastName = userInfo.lastName,
                country = null
        )
    }
}
