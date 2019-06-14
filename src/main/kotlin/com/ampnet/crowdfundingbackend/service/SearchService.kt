package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.Project

interface SearchService {
    fun searchOrganizations(name: String): List<Organization>
    fun searchProjects(name: String): List<Project>
}
