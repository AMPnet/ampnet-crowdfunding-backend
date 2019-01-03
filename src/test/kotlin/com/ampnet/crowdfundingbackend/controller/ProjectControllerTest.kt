package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.request.ProjectRequest
import com.ampnet.crowdfundingbackend.controller.pojo.response.ProjectResponse
import com.ampnet.crowdfundingbackend.enums.Currency
import com.ampnet.crowdfundingbackend.enums.OrganizationRoleType
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.security.WithMockCrowdfoundUser
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.ZonedDateTime

class ProjectControllerTest : ControllerTestBase() {

    private val projectPath = "/project"

    private val user: User by lazy {
        databaseCleanerService.deleteAllUsers()
        createUser(defaultEmail)
    }
    private val organization: Organization by lazy {
        databaseCleanerService.deleteAllOrganizations()
        createOrganization("Test organization", user)
    }

    private lateinit var testContext: TestContext

    @BeforeEach
    fun initializeTextContext() {
        organization.id
        testContext = TestContext()
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustReturnProject() {
        suppose("Project exists") {
            databaseCleanerService.deleteAllProjects()
            testContext.project = createProject("My project", organization, user)
        }

        verify("Project response is valid") {
            val result = mockMvc.perform(get("$projectPath/${testContext.project.id}"))
                    .andExpect(status().isOk)
                    .andReturn()

            val projectResponse: ProjectResponse = objectMapper.readValue(result.response.contentAsString)
            assertSoftly {
                it.assertThat(projectResponse.id).isEqualTo(testContext.project.id)
                it.assertThat(projectResponse.name).isEqualTo(testContext.project.name)
                it.assertThat(projectResponse.description).isEqualTo(testContext.project.description)
                it.assertThat(projectResponse.location).isEqualTo(testContext.project.location)
                it.assertThat(projectResponse.locationText).isEqualTo(testContext.project.locationText)
                it.assertThat(projectResponse.returnToInvestment).isEqualTo(testContext.project.returnToInvestment)
                it.assertThat(projectResponse.startDate).isEqualTo(testContext.project.startDate)
                it.assertThat(projectResponse.endDate).isEqualTo(testContext.project.endDate)
                it.assertThat(projectResponse.expectedFunding).isEqualTo(testContext.project.expectedFunding)
                it.assertThat(projectResponse.currency).isEqualTo(testContext.project.currency)
                it.assertThat(projectResponse.minPerUser).isEqualTo(testContext.project.minPerUser)
                it.assertThat(projectResponse.maxPerUser).isEqualTo(testContext.project.maxPerUser)
                it.assertThat(projectResponse.mainImage).isEqualTo(testContext.project.mainImage)
                it.assertThat(projectResponse.gallery).isEqualTo(testContext.project.gallery.orEmpty())
                it.assertThat(projectResponse.active).isEqualTo(testContext.project.active)
                it.assertThat(projectResponse.createByUser).isEqualTo(testContext.project.createdBy.getFullName())
                it.assertThat(projectResponse.organization.id).isEqualTo(organization.id)
                it.assertThat(projectResponse.organization.name).isEqualTo(organization.name)
            }

            assertThat(projectResponse.currentFunding).isEqualTo(0)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustReturnNotFoundForMissingProject() {
        verify("Controller returns not found") {
            mockMvc.perform(get("$projectPath/0"))
                    .andExpect(status().isNotFound)
        }
    }

    @Test
    @WithMockCrowdfoundUser(email = "missing@user.com")
    fun mustReturnErrorForNonExistingUser() {
        verify("Controller will return error for missing user") {
            val request = createProjectRequest(organization.id, "Error project")
            val response = mockMvc.perform(
                    post(projectPath)
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andReturn()
            verifyResponseErrorCode(response, ErrorCode.USER_MISSING)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustReturnErrorForMissingOrganization() {
        suppose("User is not a member of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
        }

        verify("Controller will return forbidden for missing organization membership") {
            val request = createProjectRequest(0, "Error project")
            mockMvc.perform(
                    post(projectPath)
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustReturnErrorForUserWithoutOrganizationMembership() {
        verify("Controller will forbidden for user without membership to create project") {
            val request = createProjectRequest(organization.id, "Error project")
            mockMvc.perform(
                    post(projectPath)
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustReturnErrorForUserOrganizationMembership() {
        suppose("User is a member of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(user.id, organization.id, OrganizationRoleType.ORG_MEMBER)
        }

        verify("Controller will return forbidden for missing organization membership") {
            val request = createProjectRequest(organization.id, "Error project")
            mockMvc.perform(
                    post(projectPath)
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToCreateProject() {
        suppose("User is an admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(user.id, organization.id, OrganizationRoleType.ORG_ADMIN)
        }

        verify("Controller will return create project") {
            testContext.projectRequest = createProjectRequest(organization.id, "Das project")
            val result = mockMvc.perform(
                    post(projectPath)
                            .content(objectMapper.writeValueAsString(testContext.projectRequest))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andReturn()

            val projectResponse: ProjectResponse = objectMapper.readValue(result.response.contentAsString)
            assertSoftly {
                it.assertThat(projectResponse.id).isNotNull
                it.assertThat(projectResponse.name).isEqualTo(testContext.projectRequest.name)
                it.assertThat(projectResponse.description).isEqualTo(testContext.projectRequest.description)
                it.assertThat(projectResponse.location).isEqualTo(testContext.projectRequest.location)
                it.assertThat(projectResponse.locationText).isEqualTo(testContext.projectRequest.locationText)
                it.assertThat(projectResponse.returnToInvestment)
                        .isEqualTo(testContext.projectRequest.returnToInvestment)

                it.assertThat(projectResponse.startDate).isEqualTo(testContext.projectRequest.startDate)
                it.assertThat(projectResponse.endDate).isEqualTo(testContext.projectRequest.endDate)
                it.assertThat(projectResponse.expectedFunding)
                        .isEqualTo(testContext.projectRequest.expectedFunding)

                it.assertThat(projectResponse.currency).isEqualTo(testContext.projectRequest.currency)
                it.assertThat(projectResponse.minPerUser).isEqualTo(testContext.projectRequest.minPerUser)
                it.assertThat(projectResponse.maxPerUser).isEqualTo(testContext.projectRequest.maxPerUser)
                it.assertThat(projectResponse.active).isEqualTo(testContext.projectRequest.active)
                it.assertThat(projectResponse.mainImage).isNullOrEmpty()
                it.assertThat(projectResponse.gallery).isNullOrEmpty()
                it.assertThat(projectResponse.createByUser).isEqualTo(user.getFullName())
                it.assertThat(projectResponse.organization.id).isEqualTo(organization.id)
                it.assertThat(projectResponse.organization.name).isEqualTo(organization.name)
            }
            testContext.projectId = projectResponse.id
        }
        verify("Project is stored in database") {
            val optionalProject = projectRepository.findByIdWithOrganizationAndCreator(testContext.projectId)
            assertThat(optionalProject).isPresent
        }
    }

    private fun createProjectRequest(organizationId: Int, name: String): ProjectRequest {
        val time = ZonedDateTime.now()
        return ProjectRequest(
                organizationId,
                name,
                "description",
                "location",
                "locationText",
                "1%-100%",
                time,
                time.plusDays(30),
                1_000_000,
                Currency.EUR,
                1,
                1_000_000,
                true
        )
    }

    private class TestContext {
        lateinit var project: Project
        lateinit var projectRequest: ProjectRequest
        var projectId: Int = -1
    }
}
