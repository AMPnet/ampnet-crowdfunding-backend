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
import com.ampnet.crowdfundingbackend.controller.pojo.response.TransactionResponse
import com.ampnet.crowdfundingbackend.enums.TransactionType
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.service.pojo.GenerateProjectWalletRequest
import com.ampnet.crowdfundingbackend.service.pojo.TransactionData
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.ZonedDateTime

class WalletControllerTest : ControllerTestBase() {

    private val walletPath = "/wallet"
    private val projectWalletPath = "/wallet/project"
    private val organizationWalletPath = "/wallet/organization"

    private lateinit var testData: TestData
    private lateinit var user: User

    @BeforeEach
    fun initTestData() {
        databaseCleanerService.deleteAllWalletsAndOwners()
        testData = TestData()
        user = createUser("test@test.com")
    }

    /* User Wallet */
    @Test
    @WithMockCrowdfoundUser(email = "test@test.com")
    fun mustBeAbleToGetOwnWallet() {
        suppose("User wallet exists") {
            testData.wallet = createWalletForUser(user, testData.hash)
        }
        suppose("User has some funds on wallet") {
            testData.balance = 100_00
            Mockito.`when`(blockchainService.getBalance(testData.hash)).thenReturn(testData.balance)
        }

        verify("Controller returns user wallet response") {
            val result = mockMvc.perform(get(walletPath))
                    .andExpect(status().isOk)
                    .andReturn()

            val walletResponse: WalletResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(walletResponse.id).isEqualTo(testData.wallet.id)
            assertThat(walletResponse.hash).isEqualTo(testData.hash)
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
        suppose("Blockchain service successfully adds wallet") {
            Mockito.`when`(blockchainService.addWallet(testData.address, testData.publicKey))
                    .thenReturn(testData.hash)
        }

        verify("User can create a wallet") {
            val request = WalletCreateRequest(testData.address, testData.publicKey)
            val result = mockMvc.perform(
                    post(walletPath)
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk)
                    .andReturn()

            val walletResponse: WalletResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(walletResponse.id).isNotNull()
            assertThat(walletResponse.hash).isEqualTo(testData.hash)
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
            assertThat(wallet.hash).isEqualTo(testData.hash)
        }
    }

    @Test
    @WithMockCrowdfoundUser(email = "test@test.com")
    fun mustNotBeAbleToCreateAdditionalWallet() {
        suppose("User wallet exists") {
            testData.wallet = createWalletForUser(user, testData.address)
        }

        verify("User cannot create a wallet") {
            val request = WalletCreateRequest(testData.address, testData.publicKey)
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
            val request = WalletCreateRequest("0x00", testData.publicKey)
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

    /* Project Wallet */
    @Test
    @WithMockCrowdfoundUser(email = "test@test.com")
    fun mustBeAbleToGetCreateProjectWalletTransaction() {
        suppose("Project exists") {
            testData.organization = createOrganization("Org test", user)
            testData.project = createProject("Test project", testData.organization, user)
        }
        suppose("User has a wallet") {
            user.wallet = createWalletForUser(user, testData.address)
        }
        suppose("Organization has a wallet") {
            createWalletForOrganization(testData.organization, testData.hash)
        }
        suppose("Blockchain service successfully generates transaction to create project wallet") {
            testData.transactionData = generateTransactionData(testData.signedTransaction)
            val request = GenerateProjectWalletRequest(
                    testData.project,
                    testData.project.organization.wallet!!.hash,
                    user.wallet!!.hash
            )
            Mockito.`when`(blockchainService.generateProjectWalletTransaction(request))
                    .thenReturn(testData.transactionData)
        }

        verify("User can get transaction to sign") {
            val result = mockMvc.perform(
                    get("$projectWalletPath/${testData.project.id}/transaction"))
                    .andExpect(status().isOk)
                    .andReturn()

            val transactionResponse: TransactionResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(transactionResponse.tx).isEqualTo(testData.transactionData)
            assertThat(transactionResponse.txId).isNotNull()
            assertThat(transactionResponse.info.txType).isEqualTo(TransactionType.CREATE_PROJECT)
        }
    }

    @Test
    @WithMockCrowdfoundUser(email = "test@test.com")
    fun mustNotBeAbleToGetCreateProjectWalletTransactionIfWalletExits() {
        suppose("Project exists") {
            val organization = createOrganization("Org test", user)
            testData.project = createProject("Test project", organization, user)
        }
        suppose("Project wallet exists") {
            testData.wallet = createWalletForProject(testData.project, testData.hash)
        }

        verify("User cannot get create wallet transaction") {
            val response = mockMvc.perform(
                    get("$projectWalletPath/${testData.project.id}/transaction"))
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
    fun mustBeAbleToGetProjectWallet() {
        suppose("Project exists") {
            val organization = createOrganization("Org test", user)
            testData.project = createProject("Test project", organization, user)
        }
        suppose("Project wallet exists") {
            testData.wallet = createWalletForProject(testData.project, testData.hash)
        }

        verify("User can get wallet") {
            val result = mockMvc.perform(get("$projectWalletPath/${testData.project.id}"))
                    .andExpect(status().isOk)
                    .andReturn()

            val walletResponse: WalletResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(walletResponse.id).isEqualTo(testData.wallet.id)
            assertThat(walletResponse.hash).isEqualTo(testData.hash)
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
    fun mustNotBeAbleToGetCreateWalletTransactionIfUserDidNotCreatedProject() {
        suppose("Project exists") {
            val creator = createUser("creator@gmail.com")
            val organization = createOrganization("Org test", creator)
            testData.project = createProject("Test project", organization, creator)
        }

        verify("User will get forbidden response") {
            mockMvc.perform(
                    get("$projectWalletPath/${testData.project.id}/transaction"))
                    .andExpect(status().isForbidden)
        }
    }

    @Test
    @WithMockCrowdfoundUser(email = "test@test.com")
    fun mustThrowExceptionIfUserTriesToGenerateProjectWalletForNonExistingProject() {
        verify("System will throw error for missing project") {
            val response = mockMvc.perform(
                    get("$projectWalletPath/0/transaction"))
                    .andExpect(status().isBadRequest)
                    .andReturn()
            verifyResponseErrorCode(response, ErrorCode.PRJ_MISSING)
        }
    }

    @Test
    @WithMockCrowdfoundUser(email = "test@test.com")
    fun mustThrowExceptionIfUserTriesToGetProjectWalletForNonExistingProject() {
        verify("System will throw error for missing project") {
            val response = mockMvc.perform(
                    get("$projectWalletPath/0"))
                    .andExpect(status().isBadRequest)
                    .andReturn()
            verifyResponseErrorCode(response, ErrorCode.PRJ_MISSING)
        }
    }

    /* Organization Wallet */
    @Test
    @WithMockCrowdfoundUser(email = "test@test.com")
    fun mustBeAbleToGetOrganizationWallet() {
        suppose("Organization exists") {
            testData.organization = createOrganization("Org test", user)
        }
        suppose("Organization has a wallet") {
            testData.wallet = createWalletForOrganization(testData.organization, testData.hash)
        }

        verify("User can fetch organization wallet") {
            val result = mockMvc.perform(
                    get("$organizationWalletPath/${testData.organization.id}"))
                    .andExpect(status().isOk)
                    .andReturn()

            val walletResponse: WalletResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(walletResponse.id).isEqualTo(testData.wallet.id)
            assertThat(walletResponse.hash).isEqualTo(testData.hash)
            assertThat(walletResponse.currency).isEqualTo(testData.wallet.currency)
            assertThat(walletResponse.type).isEqualTo(testData.wallet.type)
            assertThat(walletResponse.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
        }
    }

    @Test
    @WithMockCrowdfoundUser(email = "test@test.com")
    fun mustThrowExceptionIfOrganizationIsMissing() {
        verify("System will throw error for missing organization") {
            val response = mockMvc.perform(
                    get("$organizationWalletPath/0"))
                    .andExpect(status().isBadRequest)
                    .andReturn()
            verifyResponseErrorCode(response, ErrorCode.ORG_MISSING)
        }
    }

    @Test
    @WithMockCrowdfoundUser(email = "test@test.com")
    fun mustThrowExceptionIfUserWalletIsMissing() {
        suppose("Organization exists") {
            testData.organization = createOrganization("Turk org", user)
        }

        verify("System will throw error for missing user wallet") {
            val response = mockMvc.perform(
                    get("$organizationWalletPath/${testData.organization.id}"))
                    .andExpect(status().isBadRequest)
                    .andReturn()
            verifyResponseErrorCode(response, ErrorCode.WALLET_MISSING)
        }
    }

    @Test
    @WithMockCrowdfoundUser(email = "test@test.com")
    fun mustBeAbleToGetCreateOrganizationWallet() {
        suppose("User wallet exists") {
            testData.wallet = createWalletForUser(user, testData.address)
        }
        suppose("Organization exists") {
            testData.organization = createOrganization("Turk org", user)
        }
        suppose("Blockchain service successfully creates organization") {
            testData.transactionData = generateTransactionData(testData.signedTransaction)
            Mockito.`when`(blockchainService.generateAddOrganizationTransaction(
                    testData.wallet.hash, testData.organization.name)
            ).thenReturn(testData.transactionData)
        }

        verify("User can get transaction create organization wallet") {
            val path = "$organizationWalletPath/${testData.organization.id}/transaction"
            val result = mockMvc.perform(
                    get(path))
                    .andExpect(status().isOk)
                    .andReturn()

            val transactionResponse: TransactionResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(transactionResponse.tx).isEqualTo(testData.transactionData)
            assertThat(transactionResponse.txId).isNotNull()
            assertThat(transactionResponse.info.txType).isEqualTo(TransactionType.CREATE_ORG)
        }
    }

    @Test
    @WithMockCrowdfoundUser(email = "test@test.com")
    fun mustThrowErrorIfOrganizationIsMissingForCreateOrganizationWallet() {
        verify("System will throw error for missing organization") {
            val response = mockMvc.perform(
                    get("$organizationWalletPath/0/transaction"))
                    .andExpect(status().isBadRequest)
                    .andReturn()
            verifyResponseErrorCode(response, ErrorCode.ORG_MISSING)
        }
    }

    private fun generateTransactionData(data: String): TransactionData {
        return TransactionData(data, "to", 1, 1, 1, 1, "public_key")
    }

    private class TestData {
        lateinit var wallet: Wallet
        lateinit var project: Project
        lateinit var transactionData: TransactionData
        lateinit var organization: Organization
        var walletId = -1
        var address = "0x14bC6a8219c798394726f8e86E040A878da1d99D"
        var hash = "0x4e4ee58ff3a9e9e78c2dfdbac0d1518e4e1039f9189267e1dc8d3e35cbdf7892"
        val publicKey = "0xC2D7CF95645D33006175B78989035C7c9061d3F9"
        var balance: Long = -1
        val signedTransaction = "SignedTransaction"
    }
}
