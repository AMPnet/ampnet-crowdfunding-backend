package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.response.ProjectListResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.ProjectWithFundingResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.WalletResponse
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.persistence.model.Document
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.Wallet
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.ZonedDateTime

class ProjectPublicControllerTest : ControllerTestBase() {

    private val projectPublicPath = "/public/project"
    private val projectWalletPublicPath = "/public/wallet/project"

    private lateinit var organization: Organization
    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllProjects()
        databaseCleanerService.deleteAllOrganizations()
        databaseCleanerService.deleteAllWallets()
        organization = createOrganization("Test organization", userUuid)
        createWalletForOrganization(organization, "0xc5825e732eda043b83ea19a3a1bd2f27a65d11d6e887fa52763bb069977aa292")
        testContext = TestContext()
    }

    @Test
    fun mustBeAbleToGetAllActiveProjectsWithWallet() {
        suppose("Project with wallet exists") {
            testContext.project = createProject("My project", organization, userUuid)
            createWalletForProject(testContext.project, "0x430534053405340534")
        }
        suppose("Another organization has project without wallet") {
            val secondOrganization = createOrganization("Second organization", userUuid)
            createWalletForOrganization(secondOrganization,
                    "0xacv23e732eda043b83ea19a3a1bd2f27a65d11d6e887fa52763bb069977aa292")
            testContext.secondProject = createProject("Second project", secondOrganization, userUuid)
        }
        suppose("There is inactive project") {
            createProject("Inactive project", organization, userUuid, active = false)
        }

        verify("Controller will return all projects") {
            val result = mockMvc.perform(get(projectPublicPath))
                    .andExpect(status().isOk)
                    .andReturn()

            val projectsResponse: ProjectListResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(projectsResponse.projects).hasSize(1)
            val project = projectsResponse.projects.first()
            assertThat(project.id).isEqualTo(testContext.project.id)
            assertThat(project.active).isTrue()
            assertThat(project.walletHash).isNotEmpty()
        }
    }

    @Test
    fun mustBeAbleToGetSpecificProject() {
        suppose("Project exists") {
            testContext.project = createProject("My project", organization, userUuid)
        }

        verify("Project response is valid") {
            val result = mockMvc.perform(get("$projectPublicPath/${testContext.project.id}"))
                    .andExpect(status().isOk)
                    .andReturn()

            val projectResponse: ProjectWithFundingResponse = objectMapper.readValue(result.response.contentAsString)
            SoftAssertions.assertSoftly {
                it.assertThat(projectResponse.id).isEqualTo(testContext.project.id)
                it.assertThat(projectResponse.name).isEqualTo(testContext.project.name)
                it.assertThat(projectResponse.description).isEqualTo(testContext.project.description)
                it.assertThat(projectResponse.location).isEqualTo(testContext.project.location)
                it.assertThat(projectResponse.locationText).isEqualTo(testContext.project.locationText)
                it.assertThat(projectResponse.returnOnInvestment).isEqualTo(testContext.project.returnOnInvestment)
                it.assertThat(projectResponse.startDate).isEqualTo(testContext.project.startDate)
                it.assertThat(projectResponse.endDate).isEqualTo(testContext.project.endDate)
                it.assertThat(projectResponse.expectedFunding).isEqualTo(testContext.project.expectedFunding)
                it.assertThat(projectResponse.currency).isEqualTo(testContext.project.currency)
                it.assertThat(projectResponse.minPerUser).isEqualTo(testContext.project.minPerUser)
                it.assertThat(projectResponse.maxPerUser).isEqualTo(testContext.project.maxPerUser)
                it.assertThat(projectResponse.mainImage).isEqualTo(testContext.project.mainImage)
                it.assertThat(projectResponse.gallery).isEqualTo(testContext.project.gallery.orEmpty())
                it.assertThat(projectResponse.news).isEqualTo(testContext.project.newsLinks.orEmpty())
                it.assertThat(projectResponse.active).isEqualTo(testContext.project.active)
                it.assertThat(projectResponse.organization.id).isEqualTo(organization.id)
                it.assertThat(projectResponse.organization.name).isEqualTo(organization.name)
                it.assertThat(projectResponse.organization.legalInfo).isEqualTo(organization.legalInfo)
                it.assertThat(projectResponse.organization.approved).isEqualTo(organization.approved)
            }

            assertThat(projectResponse.walletHash).isNull()
            assertThat(projectResponse.currentFunding).isNull()
        }
    }

    @Test
    fun mustGetNotFoundForNonExistingProject() {
        verify("User will get not found for fetching non-existing project") {
            mockMvc.perform(get("$projectPublicPath/0"))
                    .andExpect(status().isNotFound)
        }
    }

    @Test
    fun mustBeAbleToGetProjectWithDocumentsAndFunding() {
        suppose("Project exists") {
            testContext.project = createProject("My project", organization, userUuid)
        }
        suppose("Project has a document") {
            testContext.document =
                    createProjectDocument(testContext.project, userUuid, "Prj doc", testContext.documentLink)
        }
        suppose("Project has a wallet") {
            createWalletForProject(testContext.project, testContext.walletHash)
        }
        suppose("Blockchain service will return current funding") {
            Mockito.`when`(blockchainService.getBalance(testContext.walletHash)).thenReturn(testContext.walletBalance)
        }

        verify("Project response contains all data") {
            val result = mockMvc.perform(get("$projectPublicPath/${testContext.project.id}"))
                    .andExpect(status().isOk)
                    .andReturn()

            val projectResponse: ProjectWithFundingResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(projectResponse.documents).hasSize(1)
            val documentResponse = projectResponse.documents[0]
            assertThat(documentResponse.id).isEqualTo(testContext.document.id)
            assertThat(documentResponse.link).isEqualTo(testContext.document.link)
            assertThat(documentResponse.type).isEqualTo(testContext.document.type)
            assertThat(documentResponse.size).isEqualTo(testContext.document.size)
            assertThat(documentResponse.name).isEqualTo(testContext.document.name)
            assertThat(documentResponse.createdAt).isEqualTo(testContext.document.createdAt)

            assertThat(projectResponse.currentFunding).isEqualTo(testContext.walletBalance)
        }
    }

    /* Wallet */
    @Test
    fun mustBeAbleToGetProjectWallet() {
        suppose("Project exists") {
            val organization = createOrganization("Org test", userUuid)
            testContext.project = createProject("Test project", organization, userUuid)
        }
        suppose("Project wallet exists") {
            testContext.wallet = createWalletForProject(testContext.project, testContext.walletHash)
        }

        verify("User can get wallet") {
            val result = mockMvc.perform(get("$projectWalletPublicPath/${testContext.project.id}"))
                    .andExpect(status().isOk)
                    .andReturn()

            val walletResponse: WalletResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(walletResponse.id).isEqualTo(testContext.wallet.id)
            assertThat(walletResponse.hash).isEqualTo(testContext.walletHash)
            assertThat(walletResponse.currency).isEqualTo(testContext.wallet.currency)
            assertThat(walletResponse.type).isEqualTo(testContext.wallet.type)
            assertThat(walletResponse.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
        }
    }

    @Test
    fun mustGetNotFoundIfWalletIsMissing() {
        suppose("Project exists") {
            val organization = createOrganization("Org test", userUuid)
            testContext.project = createProject("Test project", organization, userUuid)
        }

        verify("User will get not found") {
            mockMvc.perform(get("$projectWalletPublicPath/${testContext.project.id}"))
                    .andExpect(status().isNotFound)
        }
    }

    @Test
    fun mustThrowExceptionIfUserTriesToGetProjectWalletForNonExistingProject() {
        verify("System will throw error for missing project") {
            val response = mockMvc.perform(
                    get("$projectWalletPublicPath/0"))
                    .andExpect(status().isBadRequest)
                    .andReturn()
            verifyResponseErrorCode(response, ErrorCode.PRJ_MISSING)
        }
    }

    private class TestContext {
        lateinit var project: Project
        lateinit var secondProject: Project
        lateinit var document: Document
        lateinit var wallet: Wallet
        val documentLink = "link"
        val walletHash = "0x14bC6a8219c798394726f8e86E040A878da1d99D"
        val walletBalance = 100L
    }
}
