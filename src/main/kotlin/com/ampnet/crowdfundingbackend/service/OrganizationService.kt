package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.controller.pojo.request.OrganizationInviteRequest
import com.ampnet.crowdfundingbackend.enums.OrganizationRoleType
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.OrganizationFollower
import com.ampnet.crowdfundingbackend.persistence.model.OrganizationInvite
import com.ampnet.crowdfundingbackend.persistence.model.OrganizationMembership
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.service.pojo.OrganizationServiceRequest

interface OrganizationService {
    fun createOrganization(serviceRequest: OrganizationServiceRequest): Organization
    fun getAllOrganizations(): List<Organization>
    fun findOrganizationById(id: Int): Organization?
    fun approveOrganization(organizationId: Int, approve: Boolean, approvedBy: User): Organization
    fun findAllUsersFromOrganization(organizationId: Int): List<User>
    fun findAllOrganizationsForUser(userId: Int): List<Organization>
    fun getOrganizationMemberships(organizationId: Int): List<OrganizationMembership>
    fun addUserToOrganization(userId: Int, organizationId: Int, role: OrganizationRoleType): OrganizationMembership
    fun inviteUserToOrganization(
        request: OrganizationInviteRequest,
        organizationId: Int,
        invitedBy: User
    ): OrganizationInvite
    fun followOrganization(userId: Int, organizationId: Int): OrganizationFollower
    fun unfollowOrganization(userId: Int, organizationId: Int)
}
