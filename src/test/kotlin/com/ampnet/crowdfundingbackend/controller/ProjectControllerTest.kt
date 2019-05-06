package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.blockchain.pojo.ProjectInvestmentTxRequest
import com.ampnet.crowdfundingbackend.controller.pojo.request.ImageLinkListRequest
import com.ampnet.crowdfundingbackend.controller.pojo.request.ProjectRequest
import com.ampnet.crowdfundingbackend.controller.pojo.response.DocumentResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.ProjectListResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.ProjectResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.ProjectWithFundingResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.TransactionResponse
import com.ampnet.crowdfundingbackend.enums.Currency
import com.ampnet.crowdfundingbackend.enums.OrganizationRoleType
import com.ampnet.crowdfundingbackend.enums.TransactionType
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.persistence.model.Document
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.security.WithMockCrowdfoundUser
import com.ampnet.crowdfundingbackend.service.pojo.TransactionData
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
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

    private lateinit var organization: Organization
    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllWalletsAndOwners()
        organization = createOrganization("Test organization", user)
        createWalletForOrganization(organization, "0xc5825e732eda043b83ea19a3a1bd2f27a65d11d6e887fa52763bb069977aa292")
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

            val projectResponse: ProjectWithFundingResponse = objectMapper.readValue(result.response.contentAsString)
            assertSoftly {
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
                it.assertThat(projectResponse.active).isEqualTo(testContext.project.active)
                it.assertThat(projectResponse.createByUser).isEqualTo(testContext.project.createdBy.getFullName())
                it.assertThat(projectResponse.organization.id).isEqualTo(organization.id)
                it.assertThat(projectResponse.organization.name).isEqualTo(organization.name)
                it.assertThat(projectResponse.organization.legalInfo).isEqualTo(organization.legalInfo)
                it.assertThat(projectResponse.organization.approved).isEqualTo(organization.approved)
                it.assertThat(projectResponse.organization.createdByUser)
                        .isEqualTo(organization.createdByUser.getFullName())
            }

            assertThat(projectResponse.walletHash).isNull()
            assertThat(projectResponse.currentFunding).isNull()
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustReturnProjectWithDocumentsAndFunding() {
        suppose("Project exists") {
            databaseCleanerService.deleteAllProjects()
            testContext.project = createProject("My project", organization, user)
        }
        suppose("Project has a document") {
            testContext.document = createProjectDocument(testContext.project, user, "Prj doc", testContext.documentLink)
        }
        suppose("Project has a wallet") {
            createWalletForProject(testContext.project, testContext.walletHash)
        }
        suppose("Blockchain service will return current funding") {
            Mockito.`when`(blockchainService.getBalance(testContext.walletHash)).thenReturn(testContext.walletBalance)
        }

        verify("Project response contains all data") {
            val result = mockMvc.perform(get("$projectPath/${testContext.project.id}"))
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

            val projectResponse: ProjectWithFundingResponse = objectMapper.readValue(result.response.contentAsString)
            assertSoftly {
                it.assertThat(projectResponse.id).isNotNull
                it.assertThat(projectResponse.name).isEqualTo(testContext.projectRequest.name)
                it.assertThat(projectResponse.description).isEqualTo(testContext.projectRequest.description)
                it.assertThat(projectResponse.location).isEqualTo(testContext.projectRequest.location)
                it.assertThat(projectResponse.locationText).isEqualTo(testContext.projectRequest.locationText)
                it.assertThat(projectResponse.returnOnInvestment)
                        .isEqualTo(testContext.projectRequest.returnOnInvestment)

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
                it.assertThat(projectResponse.organization.legalInfo).isEqualTo(organization.legalInfo)
                it.assertThat(projectResponse.organization.approved).isEqualTo(organization.approved)
                it.assertThat(projectResponse.organization.createdByUser)
                        .isEqualTo(organization.createdByUser.getFullName())
            }

            assertThat(projectResponse.walletHash).isNull()
            testContext.projectId = projectResponse.id
        }
        verify("Project is stored in database") {
            val optionalProject = projectRepository.findByIdWithOrganizationAndCreator(testContext.projectId)
            assertThat(optionalProject).isPresent
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGetListOfProjectForOrganization() {
        suppose("Organization has 3 projects") {
            databaseCleanerService.deleteAllProjects()
            testContext.project = createProject("Project 1", organization, user)
            createProject("Project 2", organization, user)
            createProject("Project 3", organization, user)
        }
        suppose("Second organization has project") {
            val secondOrganization = createOrganization("Second organization", user)
            testContext.secondProject = createProject("Second project", secondOrganization, user)
        }

        verify("Controller will return all projects for specified organization") {
            val result = mockMvc.perform(get("$projectPath/organization/${organization.id}"))
                    .andExpect(status().isOk)
                    .andReturn()

            val projectListResponse: ProjectListResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(projectListResponse.projects).hasSize(3)
            assertThat(projectListResponse.projects.map { it.id }).doesNotContain(testContext.secondProject.id)

            val filterResponse = projectListResponse.projects.filter { it.id == testContext.project.id }
            assertThat(filterResponse).hasSize(1)
            testContext.projectResponse = filterResponse.first()
        }
        verify("Project response is correct") {
            assertThat(testContext.projectResponse.name).isEqualTo(testContext.project.name)
            assertThat(testContext.projectResponse.description).isEqualTo(testContext.project.description)
            assertThat(testContext.projectResponse.location).isEqualTo(testContext.project.location)
            assertThat(testContext.projectResponse.locationText).isEqualTo(testContext.project.locationText)
            assertThat(testContext.projectResponse.returnOnInvestment).isEqualTo(testContext.project.returnOnInvestment)
            assertThat(testContext.projectResponse.startDate).isEqualTo(testContext.project.startDate)
            assertThat(testContext.projectResponse.endDate).isEqualTo(testContext.project.endDate)
            assertThat(testContext.projectResponse.expectedFunding).isEqualTo(testContext.project.expectedFunding)
            assertThat(testContext.projectResponse.currency).isEqualTo(testContext.project.currency)
            assertThat(testContext.projectResponse.minPerUser).isEqualTo(testContext.project.minPerUser)
            assertThat(testContext.projectResponse.maxPerUser).isEqualTo(testContext.project.maxPerUser)
            assertThat(testContext.projectResponse.mainImage).isEqualTo(testContext.project.mainImage)
            assertThat(testContext.projectResponse.active).isEqualTo(testContext.project.active)
            assertThat(testContext.projectResponse.walletHash).isNull()
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToAddDocumentForProject() {
        suppose("Project exists") {
            databaseCleanerService.deleteAllProjects()
            testContext.project = createProject("Project", organization, user)
        }
        suppose("User is an admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(user.id, organization.id, OrganizationRoleType.ORG_ADMIN)
        }
        suppose("File service will store document") {
            testContext.multipartFile = MockMultipartFile("file", "test.txt",
                    "text/plain", "Some document data".toByteArray())
            Mockito.`when`(
                    cloudStorageService.saveFile(testContext.multipartFile.originalFilename,
                            testContext.multipartFile.bytes)
            ).thenReturn(testContext.documentLink)
        }

        verify("User can add document") {
            val result = mockMvc.perform(
                    RestDocumentationRequestBuilders.fileUpload("$projectPath/${testContext.project.id}/document")
                            .file(testContext.multipartFile))
                    .andExpect(status().isOk)
                    .andReturn()

            val documentResponse: DocumentResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(documentResponse.id).isNotNull()
            assertThat(documentResponse.name).isEqualTo(testContext.multipartFile.originalFilename)
            assertThat(documentResponse.size).isEqualTo(testContext.multipartFile.size)
            assertThat(documentResponse.type).isEqualTo(testContext.multipartFile.contentType)
            assertThat(documentResponse.link).isEqualTo(testContext.documentLink)
        }
        verify("Document is stored in database and connected to project") {
            val optionalProject = projectRepository.findByIdWithAllData(testContext.project.id)
            assertThat(optionalProject).isPresent
            val projectWithDocument = optionalProject.get()
            assertThat(projectWithDocument.documents).hasSize(1)

            val document = projectWithDocument.documents!![0]
            assertThat(document.name).isEqualTo(testContext.multipartFile.originalFilename)
            assertThat(document.size).isEqualTo(testContext.multipartFile.size)
            assertThat(document.type).isEqualTo(testContext.multipartFile.contentType)
            assertThat(document.link).isEqualTo(testContext.documentLink)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToRemoveProjectDocument() {
        suppose("Project exists") {
            databaseCleanerService.deleteAllProjects()
            testContext.project = createProject("Project", organization, user)
        }
        suppose("User is an admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(user.id, organization.id, OrganizationRoleType.ORG_ADMIN)
        }
        suppose("Project has some documents") {
            testContext.document = createProjectDocument(testContext.project, user, "Prj doc", testContext.documentLink)
            createProjectDocument(testContext.project, user, "Sec.pdf", "Sec-some-link.pdf")
        }

        verify("User admin can delete document") {
            mockMvc.perform(
                    delete("$projectPath/${testContext.project.id}/document/${testContext.document.id}"))
                    .andExpect(status().isOk)
        }
        verify("Document is deleted") {
            val project = projectRepository.findByIdWithAllData(testContext.project.id)
            assertThat(project).isPresent
            val documents = project.get().documents
            assertThat(documents).hasSize(1).doesNotContain(testContext.document)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToAddMainImage() {
        suppose("Project exists") {
            databaseCleanerService.deleteAllProjects()
            testContext.project = createProject("Project", organization, user)
        }
        suppose("User is an admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(user.id, organization.id, OrganizationRoleType.ORG_ADMIN)
        }
        suppose("File service will store image") {
            testContext.multipartFile = MockMultipartFile("image", "image.png",
                    "image/png", "ImageData".toByteArray())
            Mockito.`when`(
                    cloudStorageService.saveFile(testContext.multipartFile.originalFilename,
                            testContext.multipartFile.bytes)
            ).thenReturn(testContext.imageLink)
        }

        verify("User can add main image") {
            mockMvc.perform(
                    RestDocumentationRequestBuilders.fileUpload("$projectPath/${testContext.project.id}/image/main")
                            .file(testContext.multipartFile))
                    .andExpect(status().isOk)
        }
        verify("Document is stored in database and connected to project") {
            val optionalProject = projectRepository.findByIdWithAllData(testContext.project.id)
            assertThat(optionalProject).isPresent
            assertThat(optionalProject.get().mainImage).isEqualTo(testContext.imageLink)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToAddGalleryImage() {
        suppose("Project exists") {
            databaseCleanerService.deleteAllProjects()
            testContext.project = createProject("Project", organization, user)
        }
        suppose("User is an admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(user.id, organization.id, OrganizationRoleType.ORG_ADMIN)
        }
        suppose("File service will store image") {
            testContext.multipartFile = MockMultipartFile("image", "image.png",
                    "image/png", "ImageData".toByteArray())
            Mockito.`when`(
                    cloudStorageService.saveFile(testContext.multipartFile.originalFilename,
                            testContext.multipartFile.bytes)
            ).thenReturn(testContext.imageLink)
        }

        verify("User can add main image") {
            mockMvc.perform(
                    RestDocumentationRequestBuilders.fileUpload("$projectPath/${testContext.project.id}/image/gallery")
                            .file(testContext.multipartFile))
                    .andExpect(status().isOk)
        }
        verify("Document is stored in database and connected to project") {
            val optionalProject = projectRepository.findByIdWithAllData(testContext.project.id)
            assertThat(optionalProject).isPresent
            assertThat(optionalProject.get().gallery).contains(testContext.imageLink)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToRemoveGalleryImage() {
        suppose("Project exists") {
            databaseCleanerService.deleteAllProjects()
            testContext.project = createProject("Project", organization, user)
        }
        suppose("User is an admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            addUserToOrganization(user.id, organization.id, OrganizationRoleType.ORG_ADMIN)
        }
        suppose("Project has gallery images") {
            testContext.project.gallery = listOf("image-link-1", "image-link-2", "image-link-3")
            projectRepository.save(testContext.project)
        }

        verify("User can add main image") {
            val request = ImageLinkListRequest(listOf("image-link-1"))
            mockMvc.perform(
                    delete("$projectPath/${testContext.project.id}/image/gallery")
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk)
        }
        verify("Document is stored in database and connected to project") {
            val optionalProject = projectRepository.findByIdWithAllData(testContext.project.id)
            assertThat(optionalProject).isPresent
            assertThat(optionalProject.get().gallery).contains("image-link-2", "image-link-3")
            assertThat(optionalProject.get().gallery).doesNotContain("image-link-1")
        }
    }

    @Test
    @WithMockCrowdfoundUser(email = "user@with.wallet")
    fun mustBeAbleToGenerateInvestmentTransaction() {
        suppose("Project exists") {
            databaseCleanerService.deleteAllProjects()
            testContext.project = createProject("Project", organization, user)
        }
        suppose("Project has empty wallet") {
            createWalletForProject(testContext.project, testContext.walletHash)
        }
        suppose("User has wallet") {
            val userWithWallet = createUser("user@with.wallet")
            createWalletForUser(userWithWallet, testContext.userWalletHash)
        }
        suppose("User has enough funds on wallet") {
            Mockito.`when`(blockchainService.getBalance(testContext.userWalletHash)).thenReturn(100_000_00)
        }
        suppose("Blockchain service will generate transaction") {
            Mockito.`when`(blockchainService.generateProjectInvestmentTransaction(
                ProjectInvestmentTxRequest(testContext.userWalletHash, testContext.walletHash, 1_000))
            ).thenReturn(testContext.transactionData)
        }

        verify("User can generate invest project transaction") {
            val result = mockMvc.perform(
                get("$projectPath/${testContext.project.id}/invest")
                    .param("amount", "1000"))
                .andExpect(status().isOk)
                .andReturn()

            val transactionResponse: TransactionResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(transactionResponse.tx).isEqualTo(testContext.transactionData)
            assertThat(transactionResponse.txId).isNotNull()
            assertThat(transactionResponse.info.txType).isEqualTo(TransactionType.INVEST_ALLOWANCE)
        }
    }

    @Test
    @WithMockCrowdfoundUser(email = "user@with.wallet")
    fun mustBeAbleToGenerateConfirmInvestmentTransaction() {
        suppose("Project exists") {
            databaseCleanerService.deleteAllProjects()
            testContext.project = createProject("Project", organization, user)
        }
        suppose("Project has empty wallet") {
            createWalletForProject(testContext.project, testContext.walletHash)
        }
        suppose("User has wallet") {
            val userWithWallet = createUser("user@with.wallet")
            createWalletForUser(userWithWallet, testContext.userWalletHash)
        }
        suppose("Blockchain service will generate transaction") {
            Mockito.`when`(blockchainService.generateConfirmInvestment(
                testContext.userWalletHash, testContext.walletHash)
            ).thenReturn(testContext.transactionData)
        }

        verify("User can generate invest project transaction") {
            val result = mockMvc.perform(
                get("$projectPath/${testContext.project.id}/invest/confirm"))
                .andExpect(status().isOk)
                .andReturn()

            val transactionResponse: TransactionResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(transactionResponse.tx).isEqualTo(testContext.transactionData)
            assertThat(transactionResponse.txId).isNotNull()
            assertThat(transactionResponse.info.txType).isEqualTo(TransactionType.INVEST)
        }
    }

    @Test
    @WithMockCrowdfoundUser(email = "user@with.wallet")
    fun mustNotBeAbleToGenerateConfirmInvestmentTransactionForMissingProject() {
        suppose("User has wallet") {
            val userWithWallet = createUser("user@with.wallet")
            createWalletForUser(userWithWallet, testContext.userWalletHash)
        }

        verify("User can generate invest project transaction") {
            val result = mockMvc.perform(
                get("$projectPath/0/invest/confirm"))
                .andExpect(status().isBadRequest)
                .andReturn()
            verifyResponseErrorCode(result, ErrorCode.PRJ_MISSING)
        }
    }

    @Test
    @WithMockCrowdfoundUser(email = "missing@user.com")
    fun mustReturnErrorForNonExistingUserTryingToGenerateConfirmInvestment() {
        suppose("Project exists") {
            databaseCleanerService.deleteAllProjects()
            testContext.project = createProject("Project", organization, user)
        }

        verify("Controller will return error for missing user") {
            val response = mockMvc.perform(
                get("$projectPath/${testContext.project.id}/invest/confirm"))
                .andReturn()
            verifyResponseErrorCode(response, ErrorCode.USER_MISSING)
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

    private fun createProjectDocument(
        project: Project,
        createdBy: User,
        name: String,
        link: String,
        type: String = "document/type",
        size: Int = 100
    ): Document {
        val savedDocument = saveDocument(name, link, type, size, createdBy)
        val documents = project.documents.orEmpty().toMutableList()
        documents.add(savedDocument)
        project.documents = documents
        projectRepository.save(project)
        return savedDocument
    }

    private class TestContext {
        lateinit var project: Project
        lateinit var secondProject: Project
        lateinit var projectRequest: ProjectRequest
        lateinit var projectResponse: ProjectResponse
        lateinit var multipartFile: MockMultipartFile
        lateinit var document: Document
        val documentLink = "link"
        val imageLink = "image-link"
        var projectId: Int = -1
        val walletHash = "0x14bC6a8219c798394726f8e86E040A878da1d99D"
        val walletBalance = 100L
        val userWalletHash = "0x29bC6a8219c798394726f8e86E040A878da1daAA"
        val transactionData = TransactionData("data", "to", 22, 33, 44, 1000, "pubg")
    }
}
