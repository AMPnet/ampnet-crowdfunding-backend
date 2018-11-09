package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.service.pojo.OrganizationServiceRequest

interface OrganizationService {
    fun createOrganization(serviceRequest: OrganizationServiceRequest): Organization
    fun getAllOrganizations(): List<Organization>
    fun findOrganizationById(id: Int): Organization?
}
