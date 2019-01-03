package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.request.WalletCreateRequest
import com.ampnet.crowdfundingbackend.controller.pojo.response.WalletResponse
import com.ampnet.crowdfundingbackend.enums.Currency
import com.ampnet.crowdfundingbackend.enums.WalletType
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.persistence.model.Wallet
import com.ampnet.crowdfundingbackend.security.WithMockCrowdfoundUser
import com.ampnet.crowdfundingbackend.service.BlockchainService
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.ZonedDateTime

class WalletControllerTest : ControllerTestBase() {

    private val walletPath = "/wallet"
    private val projectWalletPath = "/wallet/project"

    @Autowired
    private lateinit var blockchainService: BlockchainService

    private lateinit var testData: TestData
    private lateinit var user: User

    @BeforeEach
    fun initTestData() {
        databaseCleanerService.deleteAllWalletsAndOwners()
        testData = TestData()
        user = createUser("test@test.com")
    }

    @Test
    @WithMockCrowdfoundUser(email = "test@test.com")
    fun mustBeAbleToGetOwnWallet() {
        suppose("User wallet exists") {
            testData.wallet = createWalletForUser(user, testData.address)
        }
        suppose("User has some funds on wallet") {
            testData.balance = 100_00
            Mockito.`when`(blockchainService.getBalance(testData.address)).thenReturn(testData.balance)
        }

        verify("Controller returns user wallet response") {
            val result = mockMvc.perform(get(walletPath))
                    .andExpect(status().isOk)
                    .andReturn()

            val walletResponse: WalletResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(walletResponse.id).isEqualTo(testData.wallet.id)
            assertThat(walletResponse.address).isEqualTo(testData.address)
            assertThat(walletResponse.currency).isEqualTo(testData.wallet.currency)
            assertThat(walletResponse.type).isEqualTo(testData.wallet.type)
            assertThat(walletResponse.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())

            assertThat(walletResponse.balance).isEqualTo(testData.balance)
        }
    }

    @Test
    @WithMockCrowdfoundUser(email = "test@test.com")
    fun mustReturnNotFoundForMissingWallet() {
        verify("Controller returns 404 for missing wallet") {
            mockMvc.perform(get(walletPath))
                    .andExpect(status().isNotFound)
        }
    }

    @Test
    @WithMockCrowdfoundUser(email = "test@test.com")
    fun mustBeAbleToCreateWallet() {
        verify("User can create a wallet") {
            val request = WalletCreateRequest(testData.address)
            val result = mockMvc.perform(
                    post(walletPath)
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk)
                    .andReturn()

            val walletResponse: WalletResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(walletResponse.id).isNotNull()
            assertThat(walletResponse.address).isEqualTo(testData.address)
            assertThat(walletResponse.currency).isEqualTo(Currency.EUR)
            assertThat(walletResponse.type).isEqualTo(WalletType.USER)
            assertThat(walletResponse.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())

            testData.walletId = walletResponse.id
        }

        verify("Wallet is created") {
            val userWithWallet = userRepository.findByEmailWithWallet(user.email)
            assertThat(userWithWallet).isPresent
            assertThat(userWithWallet.get().wallet).isNotNull

            val wallet = userWithWallet.get().wallet!!
            assertThat(wallet.id).isEqualTo(testData.walletId)
            assertThat(wallet.address).isEqualTo(testData.address)
        }
    }

    @Test
    @WithMockCrowdfoundUser(email = "test@test.com")
    fun mustNotBeAbleToCreateAdditionalWallet() {
        suppose("User wallet exists") {
            testData.wallet = createWalletForUser(user, testData.address)
        }

        verify("User cannot create a wallet") {
            val request = WalletCreateRequest(testData.address)
            mockMvc.perform(
                    post(walletPath)
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest)
                    .andReturn()
        }
    }

    @Test
    @WithMockCrowdfoundUser(email = "test@test.com")
    fun mustNotBeAbleToCreateWalletWithInvalidAddress() {
        verify("User cannot create wallet with invalid wallet address") {
            val request = WalletCreateRequest("0x00")
            mockMvc.perform(
                    post(walletPath)
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest)
                    .andReturn()
        }
    }

    @Test
    @WithMockCrowdfoundUser(email = "missing@user.com")
    fun mustReturnErrorForNonExistingUser() {
        verify("Controller will return not found for missing user") {
            val response = mockMvc.perform(get(walletPath))
                    .andExpect(status().isBadRequest)
                    .andReturn()
            verifyResponseErrorCode(response, ErrorCode.USER_MISSING)
        }
    }

    @Test
    @WithMockCrowdfoundUser(email = "test@test.com")
    fun mustBeAbleToCreateProjectWallet() {
        suppose("Project exists") {
            val organization = createOrganization("Org test", user)
            testData.project = createProject("Test project", organization, user)
        }
        verify("User can create a wallet") {
            val request = WalletCreateRequest(testData.address)
            val result = mockMvc.perform(
                    post("$projectWalletPath/${testData.project.id}")
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk)
                    .andReturn()

            val walletResponse: WalletResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(walletResponse.id).isNotNull()
            assertThat(walletResponse.address).isEqualTo(testData.address)
            assertThat(walletResponse.currency).isEqualTo(Currency.EUR)
            assertThat(walletResponse.type).isEqualTo(WalletType.PROJECT)
            assertThat(walletResponse.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())

            testData.walletId = walletResponse.id
        }

        verify("Wallet is created") {
            val optionalProject = projectRepository.findByIdWithWallet(testData.project.id)
            assertThat(optionalProject).isPresent
            assertThat(optionalProject.get().wallet).isNotNull
            val projectWithWallet = optionalProject.get().wallet!!
            assertThat(projectWithWallet.id).isEqualTo(testData.walletId)
            assertThat(projectWithWallet.address).isEqualTo(testData.address)
        }
    }

    @Test
    @WithMockCrowdfoundUser(email = "test@test.com")
    fun mustNotBeAbleToCreateAdditionalProjectWallet() {
        suppose("Project exists") {
            val organization = createOrganization("Org test", user)
            testData.project = createProject("Test project", organization, user)
        }
        suppose("Project wallet exists") {
            testData.wallet = createWalletForProject(testData.project, testData.address)
        }

        verify("User cannot create a wallet") {
            val request = WalletCreateRequest(testData.address)
            val response = mockMvc.perform(
                    post("$projectWalletPath/${testData.project.id}")
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest)
                    .andReturn()
            verifyResponseErrorCode(response, ErrorCode.WALLET_EXISTS)
        }
    }

    @Test
    @WithMockCrowdfoundUser(email = "missing@user.com")
    fun mustReturnErrorForNonExistingUserTryingToCreateOrganizationWallet() {
        suppose("Project exists") {
            val organization = createOrganization("Org test", user)
            testData.project = createProject("Test project", organization, user)
        }
        verify("Controller will return not found for missing user") {
            val response = mockMvc.perform(get(projectWalletPath + "/${testData.project.id}"))
                    .andExpect(status().isBadRequest)
                    .andReturn()
            verifyResponseErrorCode(response, ErrorCode.USER_MISSING)
        }
    }

    @Test
    @WithMockCrowdfoundUser(email = "test@test.com")
    fun mustNotBeAbleToCreateProjectWalletWithInvalidAddress() {
        suppose("Project exists") {
            val organization = createOrganization("Org test", user)
            testData.project = createProject("Test project", organization, user)
        }
        verify("User cannot create project wallet with invalid wallet address") {
            val request = WalletCreateRequest("0x00")
            mockMvc.perform(
                    post("$projectWalletPath/${testData.project.id}")
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest)
                    .andReturn()
        }
    }

    @Test
    @WithMockCrowdfoundUser(email = "test@test.com")
    fun mustNotBeAbleToCreateWalletForNonExistingProject() {
        verify("User cannot create project wallet for non existing project") {
            val request = WalletCreateRequest(testData.address)
            val response = mockMvc.perform(
                    post("$projectWalletPath/0")
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest)
                    .andReturn()
            verifyResponseErrorCode(response, ErrorCode.PRJ_MISSING)
        }
    }

    @Test
    @WithMockCrowdfoundUser(email = "test@test.com")
    fun mustBeAbleToGetProjectWallet() {
        suppose("Project exists") {
            val organization = createOrganization("Org test", user)
            testData.project = createProject("Test project", organization, user)
        }
        suppose("Project wallet exists") {
            testData.wallet = createWalletForProject(testData.project, testData.address)
        }

        verify("User can get wallet") {
            val result = mockMvc.perform(get("$projectWalletPath/${testData.project.id}"))
                    .andExpect(status().isOk)
                    .andReturn()

            val walletResponse: WalletResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(walletResponse.id).isEqualTo(testData.wallet.id)
            assertThat(walletResponse.address).isEqualTo(testData.address)
            assertThat(walletResponse.currency).isEqualTo(testData.wallet.currency)
            assertThat(walletResponse.type).isEqualTo(testData.wallet.type)
            assertThat(walletResponse.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
        }
    }

    @Test
    @WithMockCrowdfoundUser(email = "test@test.com")
    fun mustGetNotFoundIfWalletIsMissing() {
        suppose("Project exists") {
            val organization = createOrganization("Org test", user)
            testData.project = createProject("Test project", organization, user)
        }

        verify("User will get not found") {
            mockMvc.perform(get("$projectWalletPath/${testData.project.id}"))
                    .andExpect(status().isNotFound)
        }
    }

    @Test
    @WithMockCrowdfoundUser(email = "test@test.com")
    fun mustNotBeAbleToGetWalletIfUserDidNotCreatedProject() {
        suppose("Project exists") {
            val creator = createUser("creator@gmail.com")
            val organization = createOrganization("Org test", creator)
            testData.project = createProject("Test project", organization, creator)
        }
        suppose("Project wallet exists") {
            testData.wallet = createWalletForProject(testData.project, testData.address)
        }

        verify("User will get forbidden response") {
            mockMvc.perform(get("$projectWalletPath/${testData.project.id}"))
                    .andExpect(status().isForbidden)
                    .andReturn()
        }
    }

    @Test
    @WithMockCrowdfoundUser(email = "test@test.com")
    fun mustNotBeAbleToCreateWalletIfUserDidNotCreatedProject() {
        suppose("Project exists") {
            val creator = createUser("creator@gmail.com")
            val organization = createOrganization("Org test", creator)
            testData.project = createProject("Test project", organization, creator)
        }

        verify("User will get forbidden response") {
            val request = WalletCreateRequest(testData.address)
            mockMvc.perform(
                    post("$projectWalletPath/${testData.project.id}")
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden)
        }
    }

    private class TestData {
        lateinit var wallet: Wallet
        lateinit var project: Project
        var walletId = -1
        var address = "0x14bC6a8219c798394726f8e86E040A878da1d99D"
        var balance: Long = -1
    }
}
