package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.service.impl.SearchServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SearchServiceTest : JpaServiceTestBase() {

    private val searchService: SearchService by lazy {
        SearchServiceImpl(organizationRepository, projectRepository)
    }

    private lateinit var testContext: TestContext

    @BeforeEach
    fun inti() {
        testContext = TestContext()
    }

    /* Organization search */
    @Test
    fun mustReturnEmptyListForNonExistingOrganization() {
        suppose("Some organizations exist") {
            databaseCleanerService.deleteAllOrganizations()
            createOrganization("Org 1", userUuid)
            createOrganization("Org 2", userUuid)
            createOrganization("Org 3", userUuid)
        }

        verify("Service will return empty list") {
            val organizations = searchService.searchOrganizations("Non existing")
            assertThat(organizations).hasSize(0)
        }
    }

    @Test
    fun mustReturnOrganizationBySearchedName() {
        suppose("Some organization exist") {
            databaseCleanerService.deleteAllOrganizations()
            createOrganization("Org 1", userUuid)
        }
        suppose("Organization by name X exist") {
            testContext.organizationName = "Das X"
            createOrganization(testContext.organizationName, userUuid)
        }

        verify("Service will find one organization") {
            val organizations = searchService.searchOrganizations(testContext.organizationName)
            assertThat(organizations).hasSize(1)
            assertThat(organizations.first().name).isEqualTo(testContext.organizationName)
        }
    }

    @Test
    fun mustReturnOrganizationBySimilarSearchedName() {
        suppose("Some organization exist") {
            databaseCleanerService.deleteAllOrganizations()
            createOrganization("Org 1", userUuid)
            createOrganization("Org 2", userUuid)
            createOrganization("Org 3", userUuid)
        }
        suppose("Organization by name X exist") {
            testContext.organizationName = "Das X"
            createOrganization(testContext.organizationName, userUuid)
        }

        verify("Service will find by name in lower case") {
            val organizations = searchService.searchOrganizations(testContext.organizationName.toLowerCase())
            assertThat(organizations).hasSize(1)
            assertThat(organizations.first().name).isEqualTo(testContext.organizationName)
        }
        verify("Service will find by similar in upper case") {
            val organizations = searchService.searchOrganizations(testContext.organizationName.toUpperCase())
            assertThat(organizations).hasSize(1)
            assertThat(organizations.first().name).isEqualTo(testContext.organizationName)
        }
        verify("Service will find by name if the last word is missing") {
            val organizations = searchService.searchOrganizations(testContext.organizationName.split(" ")[0])
            assertThat(organizations).hasSize(1)
            assertThat(organizations.first().name).isEqualTo(testContext.organizationName)
        }
        verify("Service will find by name if the first word is missing") {
            val organizations = searchService.searchOrganizations(testContext.organizationName.split(" ")[1])
            assertThat(organizations).hasSize(1)
            assertThat(organizations.first().name).isEqualTo(testContext.organizationName)
        }
        verify("Service will find multiple organizations with similar name") {
            val organizations = searchService.searchOrganizations("Org")
            assertThat(organizations).hasSize(3)
        }
    }

    /*Project search */
    @Test
    fun mustReturnEmptyListForNonExistingProject() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("org for projects", userUuid)
        }
        suppose("Some organizations exist") {
            databaseCleanerService.deleteAllProjects()
            createProject("Project 1", testContext.organization, userUuid)
            createProject("Project 2", testContext.organization, userUuid)
            createProject("Project 3", testContext.organization, userUuid)
        }

        verify("Service will return empty list") {
            val projects = searchService.searchProjects("Non existing")
            assertThat(projects).hasSize(0)
        }
    }

    @Test
    fun mustReturnProjectBySearchedName() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("org for projects", userUuid)
        }
        suppose("Some organizations exist") {
            databaseCleanerService.deleteAllProjects()
            createProject("Project 1", testContext.organization, userUuid)
        }
        suppose("Organization by name X exist") {
            testContext.projectName = "Prj with name"
            createProject(testContext.projectName, testContext.organization, userUuid)
        }

        verify("Service will find one organization") {
            val organizations = searchService.searchProjects(testContext.projectName)
            assertThat(organizations).hasSize(1)
            assertThat(organizations.first().name).isEqualTo(testContext.projectName)
        }
    }

    @Test
    fun mustReturnProjectBySimilarSearchedName() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("org for projects", userUuid)
        }
        suppose("Some organizations exist") {
            databaseCleanerService.deleteAllProjects()
            createProject("Project 1", testContext.organization, userUuid)
        }
        suppose("Organization by name X exist") {
            testContext.projectName = "Prj with name"
            createProject(testContext.projectName, testContext.organization, userUuid)
        }

        verify("Service will find by name in lower case") {
            val projects = searchService.searchProjects(testContext.projectName.toLowerCase())
            assertThat(projects).hasSize(1)
            assertThat(projects.first().name).isEqualTo(testContext.projectName)
        }
        verify("Service will find by similar in upper case") {
            val projects = searchService.searchProjects(testContext.projectName.toUpperCase())
            assertThat(projects).hasSize(1)
            assertThat(projects.first().name).isEqualTo(testContext.projectName)
        }
        verify("Service will find by name if the last word is missing") {
            val projects = searchService.searchProjects(testContext.projectName.split(" ")[0])
            assertThat(projects).hasSize(1)
            assertThat(projects.first().name).isEqualTo(testContext.projectName)
        }
        verify("Service will find by name if the first word is missing") {
            val projects = searchService.searchProjects(testContext.projectName.split(" ")[1])
            assertThat(projects).hasSize(1)
            assertThat(projects.first().name).isEqualTo(testContext.projectName)
        }
        verify("Service will return 2 projects that start with same letter") {
            val projects = searchService.searchProjects("P")
            assertThat(projects).hasSize(2)
        }
    }

    private class TestContext {
        lateinit var organizationName: String
        lateinit var organization: Organization
        lateinit var projectName: String
    }
}
