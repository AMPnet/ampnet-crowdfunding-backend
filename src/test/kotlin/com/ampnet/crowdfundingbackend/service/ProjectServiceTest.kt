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
            databaseCleanerService.deleteAllProjects()
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
            assertThat(project.active).isFalse()
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

    @Test
    fun mustBeAbleToAddMainImage() {
        suppose("Project exists") {
            databaseCleanerService.deleteAllProjects()
            testContext.createProjectRequest = createProjectRequest("Image")
            testContext.project = projectService.createProject(testContext.createProjectRequest)
        }
        suppose("Main image is added to project") {
            testContext.image = "hash-main-image"
            projectService.addMainImage(testContext.project, testContext.image)
        }

        verify("Image is stored in project") {
            val optionalProject = projectRepository.findById(testContext.project.id)
            assertThat(optionalProject).isPresent
            assertThat(optionalProject.get().mainImage).isEqualTo(testContext.image)
        }
    }

    @Test
    fun mustBeAbleToAddImagesToGallery() {
        suppose("Project exists") {
            databaseCleanerService.deleteAllProjects()
            testContext.createProjectRequest = createProjectRequest("Image")
            testContext.project = projectService.createProject(testContext.createProjectRequest)
        }
        suppose("Two images are added to project gallery") {
            testContext.gallery = listOf("hash-1", "hash-2")
            projectService.addImagesToGallery(testContext.project, testContext.gallery)
        }

        verify("The project gallery contains added images") {
            val optionalProject = projectRepository.findById(testContext.project.id)
            assertThat(optionalProject).isPresent
            assertThat(optionalProject.get().gallery).containsAll(testContext.gallery)
        }
    }

    @Test
    fun mustBeAbleToAppendNewImageToGallery() {
        suppose("Project exists") {
            databaseCleanerService.deleteAllProjects()
            testContext.createProjectRequest = createProjectRequest("Image")
            testContext.project = projectService.createProject(testContext.createProjectRequest)
        }
        suppose("The has gallery") {
            testContext.gallery = listOf("hash-1", "hash-2")
            projectService.addImagesToGallery(testContext.project, testContext.gallery)
        }
        suppose("Additional image is added to gallery") {
            testContext.image = "hash-new"
            projectService.addImagesToGallery(testContext.project, listOf(testContext.image))
        }

        verify("Gallery has additional image") {
            val optionalProject = projectRepository.findById(testContext.project.id)
            assertThat(optionalProject).isPresent
            val gallery = optionalProject.get().gallery
            assertThat(gallery).containsAll(testContext.gallery)
            assertThat(gallery).contains(testContext.image)
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
        lateinit var image: String
        lateinit var gallery: List<String>
    }
}
