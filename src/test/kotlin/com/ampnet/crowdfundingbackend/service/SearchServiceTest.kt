package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.controller.ControllerTestBase
import com.ampnet.crowdfundingbackend.persistence.model.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class SearchServiceTest : ControllerTestBase() {

    @Autowired
    private lateinit var searchService: SearchService

    private val user: User by lazy {
        databaseCleanerService.deleteAllUsers()
        createUser("adfadfaf@email.com")
    }

    private lateinit var testContext: TestContext

    @BeforeEach
    fun inti() {
        testContext = TestContext()
    }

    @Test
    fun mustReturnEmptyListForNonExistingOrganization() {
        suppose("DB is clean") {
            databaseCleanerService.deleteAllOrganizations()
        }

        suppose("Some organizations exist") {
            createOrganization("Org 1",user)
            createOrganization("Org 2",user)
            createOrganization("Org 3",user)
        }

        verify("Service will return empty list") {
            val organizations = searchService.searchOrganizations("Non existing")
            assertThat(organizations).hasSize(0)
        }
    }

    @Test
    @Disabled("Service not implemented")
    fun mustReturnOrganizationBySearchedName() {
        suppose("Multiple organization exist") {
            createOrganization("Org 1",user)
            createOrganization("Org 2",user)
            createOrganization("Org 3",user)
        }
        suppose("Organization by name X exist") {
            testContext.organizationName = "Das X"
            createOrganization(testContext.organizationName, user)
        }

        verify("Service will find one organization") {
            val organizations = searchService.searchOrganizations(testContext.organizationName)
            assertThat(organizations).hasSize(1)
            assertThat(organizations.first().name).isEqualTo(testContext.organizationName)
        }
    }

    private class TestContext {
        lateinit var organizationName: String
    }
}
