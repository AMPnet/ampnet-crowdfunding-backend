package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.response.OrganizationResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.ProjectResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.SearchOrgAndProjectResponse
import com.ampnet.crowdfundingbackend.service.SearchService
import mu.KLogging
import org.springframework.http.ResponseEntity
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
        logger.trace { "Searching for organization and projects with name: $name" }

        val organizations = searchService.searchOrganizations(name)
        logger.trace { "Fund organizations = $organizations" }
        val projects = searchService.searchProjects(name)
        logger.trace { "Found projects = $projects" }

        val organizationListResponse = organizations.map { OrganizationResponse(it) }
        val projectListResponse = projects.map { ProjectResponse(it) }
        return ResponseEntity.ok(SearchOrgAndProjectResponse(organizationListResponse, projectListResponse))
    }
}
