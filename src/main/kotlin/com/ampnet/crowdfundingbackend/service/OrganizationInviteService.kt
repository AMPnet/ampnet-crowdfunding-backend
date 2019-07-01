package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.persistence.model.OrganizationFollower
import com.ampnet.crowdfundingbackend.persistence.model.OrganizationInvitation
import com.ampnet.crowdfundingbackend.service.pojo.OrganizationInviteAnswerRequest
import com.ampnet.crowdfundingbackend.service.pojo.OrganizationInviteServiceRequest
import java.util.UUID

interface OrganizationInviteService {
    fun sendInvitation(request: OrganizationInviteServiceRequest): OrganizationInvitation
    fun revokeInvitation(organizationId: Int, email: String)
    fun getAllInvitationsForUser(email: String): List<OrganizationInvitation>
    fun answerToInvitation(request: OrganizationInviteAnswerRequest)
    fun followOrganization(userUuid: UUID, organizationId: Int): OrganizationFollower
    fun unfollowOrganization(userUuid: UUID, organizationId: Int)
}
