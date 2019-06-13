package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.enums.OrganizationRoleType
import com.ampnet.crowdfundingbackend.persistence.model.Document
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.OrganizationFollower
import com.ampnet.crowdfundingbackend.persistence.model.OrganizationInvite
import com.ampnet.crowdfundingbackend.persistence.model.OrganizationMembership
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.service.pojo.DocumentSaveRequest
import com.ampnet.crowdfundingbackend.service.pojo.OrganizationInviteServiceRequest
import com.ampnet.crowdfundingbackend.service.pojo.OrganizationServiceRequest

interface OrganizationService {
    fun createOrganization(serviceRequest: OrganizationServiceRequest): Organization
    fun getAllOrganizations(): List<Organization>
    fun findOrganizationById(id: Int): Organization?
    fun findOrganizationByIdWithWallet(id: Int): Organization?
    fun approveOrganization(organizationId: Int, approve: Boolean, approvedBy: String): Organization
    fun findAllOrganizationsForUser(userUuid: String): List<Organization>
    fun getOrganizationMemberships(organizationId: Int): List<OrganizationMembership>
    fun addUserToOrganization(userUuid: String, organizationId: Int, role: OrganizationRoleType): OrganizationMembership

    fun addDocument(organizationId: Int, request: DocumentSaveRequest): Document
    fun removeDocument(organizationId: Int, documentId: Int)
}
