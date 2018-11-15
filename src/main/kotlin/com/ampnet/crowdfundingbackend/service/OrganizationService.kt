package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.service.pojo.OrganizationServiceRequest

interface OrganizationService {
    fun createOrganization(serviceRequest: OrganizationServiceRequest): Organization
    fun getAllOrganizations(): List<Organization>
    fun findOrganizationById(id: Int): Organization?
    fun approveOrganization(organizationId: Int, approve: Boolean, approvedBy: User): Organization
    fun findAllUsersFromOrganization(organizationId: Int): List<User>
    fun findAllOrganizationsForUser(userId: Int): List<Organization>
}
