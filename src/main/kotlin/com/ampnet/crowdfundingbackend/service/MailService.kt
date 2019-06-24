package com.ampnet.crowdfundingbackend.service

interface MailService {
    fun sendOrganizationInvitationMail(to: String, organizationName: String)
}
