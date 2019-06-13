package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.response.SearchOrgAndProjectResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.UsersListResponse
import com.ampnet.crowdfundingbackend.enums.PrivilegeType
import com.ampnet.crowdfundingbackend.enums.UserRoleType
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.security.WithMockCrowdfoundUser
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class SearchControllerTest : ControllerTestBase() {

    private val searchPath = "/search"

    private lateinit var user: User

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllUsers()
        user = createUser(defaultEmail)
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

            val organization = createOrganization("The Prospect Organization", userUuid)
            createOrganization("The Organization", userUuid)

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

    @Test
    @WithMockCrowdfoundUser(role = UserRoleType.USER)
    fun mustNotBeAbleToSearchUsersWithPrivileges() {
        verify("Controller will throw forbidden") {
            mockMvc.perform(get("$searchPath/users").param("email", "Pro"))
                .andExpect(status().isForbidden)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_PROFILE])
    fun mustReturnListOfUsers() {
        suppose("Some users exist") {
            databaseCleanerService.deleteAllUsers()
            createUser("ivan.us@mail.com")
            createUser("ivana.es@gmail.com")
        }

        verify("Controller will return a list of users") {
            val result = mockMvc.perform(get("$searchPath/users").param("email", "ivan"))
                .andExpect(status().isOk)
                .andReturn()

            val searchResponse: UsersListResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(searchResponse.users).hasSize(2)
        }
    }
}
