package com.ampnet.crowdfundingbackend.service

interface MailService {
    fun sendOrganizationInvitationMail(to: String, invitedBy: String, organizationName: String)
}
