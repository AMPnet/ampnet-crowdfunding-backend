package com.ampnet.crowdfundingbackend.service

import org.springframework.social.google.api.userinfo.GoogleUserInfo

interface GoogleService {
    fun getUserProfile(token: String): GoogleUserInfo
}