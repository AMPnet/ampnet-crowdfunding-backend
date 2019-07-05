package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.service.MailService
import org.springframework.stereotype.Service

@Service
class MailServiceImpl : MailService {
    override fun sendOrganizationInvitationMail(to: String, organizationName: String) {
        // TODO: implement using mail-service
    }
}
