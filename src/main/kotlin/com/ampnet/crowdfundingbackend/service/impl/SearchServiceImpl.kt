package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.persistence.SearchDao
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.service.SearchService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SearchServiceImpl(
    private val searchDao: SearchDao
): SearchService {

    @Transactional(readOnly = true)
    override fun searchOrganizations(name: String): List<Organization> {
        return searchDao.searchOrganizationByName(name)
    }

    @Transactional(readOnly = true)
    override fun searchProjects(name: String): List<Project> {
        // TODO: implement
        return emptyList()
    }
}
