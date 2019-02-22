package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.response.OrganizationResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.ProjectResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.SearchOrgAndProjectResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.UserResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.UsersListResponse
import com.ampnet.crowdfundingbackend.service.SearchService
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class SearchController(private val searchService: SearchService) {

    companion object : KLogging()

    @GetMapping("/search")
    fun findOrganizationsAndProjects(
        @RequestParam(name = "name") name: String
    ): ResponseEntity<SearchOrgAndProjectResponse> {
        logger.debug { "Searching for organization and projects with name: $name" }

        val organizations = searchService.searchOrganizations(name)
        logger.debug { "Found organizations = ${organizations.map { it.name }}" }
        val projects = searchService.searchProjects(name)
        logger.debug { "Found projects = ${projects.map { it.name }}" }

        val organizationListResponse = organizations.map { OrganizationResponse(it) }
        val projectListResponse = projects.map { ProjectResponse(it) }
        return ResponseEntity.ok(SearchOrgAndProjectResponse(organizationListResponse, projectListResponse))
    }

    @GetMapping("/search/users")
    @PreAuthorize("hasAuthority(T(com.ampnet.crowdfundingbackend.enums.PrivilegeType).PRA_PROFILE)")
    fun findUserByEmail(@RequestParam(name = "email") email: String): ResponseEntity<UsersListResponse> {
        logger.debug { "Searching for users with email: $email" }

        val users = searchService.searchUsers(email)
        logger.debug { "Found users = ${users.map { it.email }}" }

        val userListResponse = users.map { UserResponse(it) }
        return ResponseEntity.ok(UsersListResponse(userListResponse))
    }
}
