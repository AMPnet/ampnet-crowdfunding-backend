package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.repository.OrganizationDao
import com.ampnet.crowdfundingbackend.service.OrganizationService
import com.ampnet.crowdfundingbackend.service.pojo.OrganizationServiceRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Service
class OrganizationServiceImpl(private val organizationDao: OrganizationDao) : OrganizationService {

    @Transactional
    override fun createOrganization(serviceRequest: OrganizationServiceRequest): Organization {
        val organization = Organization::class.java.newInstance()
        organization.name = serviceRequest.name
        organization.createdByUser = serviceRequest.owner
        organization.legalInfo = serviceRequest.legalInfo
        organization.documents = serviceRequest.documentHashes

        organization.approved = false
        organization.createdAt = ZonedDateTime.now()

        return organizationDao.save(organization)
    }

    @Transactional(readOnly = true)
    override fun getAllOrganizations(): List<Organization> {
        return organizationDao.findAll()
    }

    @Transactional(readOnly = true)
    override fun findOrganizationById(id: Int): Organization? {
        return ServiceUtils.wrapOptional(organizationDao.findById(id))
    }
}
