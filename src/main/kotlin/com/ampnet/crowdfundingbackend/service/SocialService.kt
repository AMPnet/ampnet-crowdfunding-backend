package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.exception.SocialException
import com.ampnet.crowdfundingbackend.service.pojo.SocialUser

interface SocialService {

    @Throws(SocialException::class)
    fun getFacebookUserInfo(token: String): SocialUser

    @Throws(SocialException::class)
    fun getGoogleUserInfo(token: String): SocialUser
}
