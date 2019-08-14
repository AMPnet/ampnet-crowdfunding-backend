package com.ampnet.crowdfundingbackend.service

interface MailService {
    fun sendOrganizationInvitationMail(email: String, organizationName: String)
}
