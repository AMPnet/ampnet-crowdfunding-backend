package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.service.GoogleService
import org.springframework.social.google.api.impl.GoogleTemplate
import org.springframework.social.google.api.userinfo.GoogleUserInfo
import org.springframework.stereotype.Service

@Service("googleService")
class GoogleServiceImpl: GoogleService {

    override fun getUserProfile(token: String): GoogleUserInfo {
        val template = GoogleTemplate(token)
        return template.userOperations().userInfo
    }
}