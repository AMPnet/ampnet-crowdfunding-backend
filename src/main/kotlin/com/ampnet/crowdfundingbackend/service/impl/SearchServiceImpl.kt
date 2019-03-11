package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.persistence.repository.OrganizationRepository
import com.ampnet.crowdfundingbackend.persistence.repository.ProjectRepository
import com.ampnet.crowdfundingbackend.persistence.repository.UserRepository
import com.ampnet.crowdfundingbackend.service.SearchService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SearchServiceImpl(
    private val organizationRepository: OrganizationRepository,
    private val projectRepository: ProjectRepository,
    private val userRepository: UserRepository
) : SearchService {

    @Transactional(readOnly = true)
    override fun searchOrganizations(name: String): List<Organization> {
        return organizationRepository.findByNameContainingIgnoreCase(name)
    }

    @Transactional(readOnly = true)
    override fun searchProjects(name: String): List<Project> {
        return projectRepository.findByNameContainingIgnoreCase(name)
    }

    @Transactional(readOnly = true)
    override fun searchUsers(email: String): List<User> {
        return userRepository.findByEmailContainingIgnoreCase(email)
    }
}
