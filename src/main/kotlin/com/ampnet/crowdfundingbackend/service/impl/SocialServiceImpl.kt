package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.persistence.repository.CountryDao
import com.ampnet.crowdfundingbackend.service.SocialService
import com.ampnet.crowdfundingbackend.service.pojo.SocialUser
import mu.KLogging
import org.springframework.social.facebook.api.Page
import org.springframework.social.facebook.api.User
import org.springframework.social.facebook.api.impl.FacebookTemplate
import org.springframework.social.google.api.impl.GoogleTemplate
import org.springframework.stereotype.Service

@Service
class SocialServiceImpl(val countryDao: CountryDao) : SocialService {

    companion object : KLogging()

    override fun getFacebookUserInfo(token: String): SocialUser {
        logger.debug { "Getting Facebook user info" }
        val facebook = FacebookTemplate(token)
        val userProfile = facebook.fetchObject(
                "me",
                User::class.java,
                "id", "email", "first_name", "last_name", "location"
        )
        logger.debug { "Received Facebook user info with mail: ${userProfile.email}" }

        var countryId: Int? = null
        if (userProfile.location != null && userProfile.location.id != null) {
            countryId = getCountryIdFromFacebook(facebook, userProfile)
        }

        return SocialUser(
                email = userProfile.email,
                firstName = userProfile.firstName,
                lastName = userProfile.lastName,
                countryId = countryId
        )
    }

    override fun getGoogleUserInfo(token: String): SocialUser {
        logger.debug { "Getting Google user info" }
        val template = GoogleTemplate(token)
        val userInfo = template.userOperations().userInfo
        logger.debug { "Received Google user info with mail: ${userInfo.email}" }
        return SocialUser(
                email = userInfo.email,
                firstName = userInfo.firstName,
                lastName = userInfo.lastName,
                countryId = null
        )
    }

    private fun getCountryIdFromFacebook(
        facebook: FacebookTemplate,
        userProfile: User
    ): Int? {
        var countryId: Int? = null

        logger.debug { "Trying to get Facebook user location." }
        /*
        This block will always throw because in order to use `Page Public Content Access`
        (which we need for location access) app has to be reviewed and approved by Facebook.
        For now, catch and do nothing.
         */
        try { // this block will always throw since facebook app still not reviewed
            val page = facebook.fetchObject(userProfile.location.id, Page::class.java, "location")
            logger.debug { "Found Facebook user location: ${page.location.country}" }

            val country = countryDao.findByNicename(page.location.country)
            if (country.isPresent) {
                countryId = country.get().id
            } else {
                logger.error { "Country from Facebook: ${page.location.country} is missing in database." }
            }
        } catch (ex: Exception) {
            logger.info("Could not fetch user from Facebook.", ex)
        }

        return countryId
    }
}
