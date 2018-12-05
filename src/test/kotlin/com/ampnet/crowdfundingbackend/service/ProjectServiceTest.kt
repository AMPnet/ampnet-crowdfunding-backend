package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.enums.Currency
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.InvalidRequestException
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.persistence.repository.ProjectRepository
import com.ampnet.crowdfundingbackend.service.impl.ProjectServiceImpl
import com.ampnet.crowdfundingbackend.service.pojo.CreateProjectServiceRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.ZonedDateTime

class ProjectServiceTest : JpaServiceTestBase() {

    @Autowired
    private lateinit var projectRepository: ProjectRepository

    private val projectService: ProjectService by lazy { ProjectServiceImpl(projectRepository) }
    private val user: User by lazy {
        databaseCleanerService.deleteAllUsers()
        createUser("test@email.com", "Test", "User")
    }
    private val organization: Organization by lazy {
        databaseCleanerService.deleteAllOrganizations()
        createOrganization("Das Organization", user)
    }

    private lateinit var testContext: TestContext

    @BeforeEach
    fun initTestContext() {
        organization.id
        testContext = TestContext()
    }

    @Test
    fun mustBeAbleToCreateProject() {
        suppose("Service received a request to create a project") {
            testContext.createProjectRequest = createProjectRequest("Test project")
            testContext.project = projectService.createProject(testContext.createProjectRequest)
        }

        verify("Project is created") {
            val optionalProject = projectRepository.findByIdWithOrganizationAndCreator(testContext.project.id)
            assertThat(optionalProject).isPresent
            val project = optionalProject.get()
            val request = testContext.createProjectRequest
            assertAll()
            assertThat(project.name).isEqualTo(request.name)
            assertThat(project.description).isEqualTo(request.description)
            assertThat(project.location).isEqualTo(request.location)
            assertThat(project.locationText).isEqualTo(request.locationText)
            assertThat(project.returnToInvestment).isEqualTo(request.returnToInvestment)
            assertThat(project.startDate).isEqualTo(request.startDate)
            assertThat(project.endDate).isEqualTo(request.endDate)
            assertThat(project.expectedFunding).isEqualByComparingTo(request.expectedFounding)
            assertThat(project.currency).isEqualTo(request.currency)
            assertThat(project.minPerUser).isEqualByComparingTo(request.minPerUser)
            assertThat(project.maxPerUser).isEqualByComparingTo(request.maxPerUser)
            assertThat(project.createdBy.id).isEqualTo(request.createdBy.id)
            assertThat(project.organization.id).isEqualTo(organization.id)
            assertThat(project.mainImage.isNullOrEmpty()).isTrue()
            assertThat(project.gallery.isNullOrEmpty()).isTrue()
            assertThat(project.documents.isNullOrEmpty()).isTrue()
        }
    }

    @Test
    fun mustNotBeAbleToCreateProjectWithEndDateBeforeStartDate() {
        suppose("Request has end date before start date") {
            testContext.createProjectRequest = CreateProjectServiceRequest(
                    organization,
                    "Invalid date",
                    "Description",
                    "location",
                    "locationText",
                    "1-2%",
                    ZonedDateTime.now(),
                    ZonedDateTime.now().minusDays(1),
                    BigDecimal("1000000"),
                    Currency.EUR,
                    BigDecimal("100"),
                    BigDecimal("10000"),
                    user
            )
        }

        verify("Service will throw an exception") {
            val exception = assertThrows<InvalidRequestException> {
                projectService.createProject(testContext.createProjectRequest)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.PRJ_DATE)
        }
    }

    private fun createProjectRequest(name: String): CreateProjectServiceRequest {
        return CreateProjectServiceRequest(
                organization,
                name,
                "Description",
                "location",
                "locationText",
                "1-2%",
                ZonedDateTime.now(),
                ZonedDateTime.now().plusDays(30),
                BigDecimal("1000000"),
                Currency.EUR,
                BigDecimal("100"),
                BigDecimal("10000"),
                user
        )
    }

    private class TestContext {
        lateinit var createProjectRequest: CreateProjectServiceRequest
        lateinit var project: Project
    }
}
