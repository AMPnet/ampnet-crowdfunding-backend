package com.ampnet.crowdfundingbackend.service

import java.util.UUID

interface MailService {
    fun sendOrganizationInvitationMail(email: String, organizationName: String)
    fun sendDepositRequest(user: UUID, amount: Long)
    fun sendDepositInfo(user: UUID, minted: Boolean)
    fun sendWithdrawRequest(user: UUID, amount: Long)
    fun sendWithdrawInfo(user: UUID, burned: Boolean)
}
