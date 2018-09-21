package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.service.pojo.SocialUser

interface SocialService {
    fun getFacebookUserInfo(token: String): SocialUser
    fun getGoogleUserInfo(token: String): SocialUser
}