package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.persistence.model.OrganizationFollower
import com.ampnet.crowdfundingbackend.persistence.model.OrganizationInvite
import com.ampnet.crowdfundingbackend.service.pojo.OrganizationInviteAnswerRequest
import com.ampnet.crowdfundingbackend.service.pojo.OrganizationInviteServiceRequest

interface OrganizationInviteService {
    fun inviteUserToOrganization(request: OrganizationInviteServiceRequest): OrganizationInvite
    fun revokeInvitationToJoinOrganization(organizationId: Int, email: String)
    fun getAllOrganizationInvitesForUser(email: String): List<OrganizationInvite>
    fun answerToOrganizationInvitation(request: OrganizationInviteAnswerRequest)
    fun followOrganization(userUuid: String, organizationId: Int): OrganizationFollower
    fun unfollowOrganization(userUuid: String, organizationId: Int)
}
