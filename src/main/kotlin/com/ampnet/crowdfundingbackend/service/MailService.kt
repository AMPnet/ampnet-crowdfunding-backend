package com.ampnet.crowdfundingbackend.service

interface MailService {
    fun sendConfirmationMail(to: String, token: String)
    fun sendOrganizationInvitationMail(to: String, invitedBy: String, organizationName: String)
}
