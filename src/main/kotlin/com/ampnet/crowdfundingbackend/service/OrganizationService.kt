package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.enums.OrganizationRoleType
import com.ampnet.crowdfundingbackend.persistence.model.Document
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.OrganizationMembership
import com.ampnet.crowdfundingbackend.service.pojo.DocumentSaveRequest
import com.ampnet.crowdfundingbackend.service.pojo.OrganizationServiceRequest
import java.util.UUID

interface OrganizationService {
    fun createOrganization(serviceRequest: OrganizationServiceRequest): Organization
    fun getAllOrganizations(): List<Organization>
    fun findOrganizationById(id: Int): Organization?
    fun findOrganizationByIdWithWallet(id: Int): Organization?
    fun approveOrganization(organizationId: Int, approve: Boolean, approvedBy: UUID): Organization
    fun findAllOrganizationsForUser(userUuid: UUID): List<Organization>

    fun getOrganizationMemberships(organizationId: Int): List<OrganizationMembership>
    fun addUserToOrganization(userUuid: UUID, organizationId: Int, role: OrganizationRoleType): OrganizationMembership
    fun removeUserFromOrganization(userUuid: UUID, organizationId: Int)

    fun addDocument(organizationId: Int, request: DocumentSaveRequest): Document
    fun removeDocument(organizationId: Int, documentId: Int)
}
