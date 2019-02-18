package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.response.SearchOrgAndProjectResponse
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class SearchControllerTest : ControllerTestBase() {

    private val searchPath = "/search"

    private val user: User by lazy {
        databaseCleanerService.deleteAllUsers()
        createUser(defaultEmail)
    }

    @Test
    @WithMockUser
    fun mustReturnEmptyListForSearch() {
        suppose("There are no projects and organizations") {
            databaseCleanerService.deleteAllOrganizations()
            databaseCleanerService.deleteAllProjects()
        }

        verify("Controller will return empty list of projects and organizations") {
            val result = mockMvc.perform(get(searchPath).param("name", "Empty"))
                .andExpect(status().isOk)
                .andReturn()

            val searchResponse: SearchOrgAndProjectResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(searchResponse.organizations).hasSize(0)
            assertThat(searchResponse.projects).hasSize(0)
        }
    }

    @Test
    @WithMockUser
    fun mustReturnListOfOrganizationsAndProjects() {
        suppose("There are projects and organizations with similar name") {
            databaseCleanerService.deleteAllOrganizations()
            databaseCleanerService.deleteAllProjects()

            val organization = createOrganization("The Prospect Organization", user)
            createOrganization("The Organization", user)

            createProject("The first project", organization, user)
            createProject("The projcccp", organization, user)
        }

        verify("Controller will a list of organizations and project containing searched word") {
            val result = mockMvc.perform(get(searchPath).param("name", "Pro"))
                .andExpect(status().isOk)
                .andReturn()

            val searchResponse: SearchOrgAndProjectResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(searchResponse.organizations).hasSize(1)
            assertThat(searchResponse.projects).hasSize(2)
        }
    }
}
