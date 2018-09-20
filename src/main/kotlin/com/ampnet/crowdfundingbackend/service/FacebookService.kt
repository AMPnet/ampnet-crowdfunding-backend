package com.ampnet.crowdfundingbackend.service

import org.springframework.social.facebook.api.User

interface FacebookService {
    fun getUserProfile(token: String): User
}