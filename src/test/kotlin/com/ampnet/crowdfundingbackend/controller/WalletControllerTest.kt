package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.request.WalletCreateRequest
import com.ampnet.crowdfundingbackend.controller.pojo.response.WalletResponse
import com.ampnet.crowdfundingbackend.enums.Currency
import com.ampnet.crowdfundingbackend.enums.WalletType
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.Wallet
import com.ampnet.crowdfundingbackend.security.WithMockCrowdfoundUser
import com.ampnet.crowdfundingbackend.controller.pojo.response.TransactionResponse
import com.ampnet.crowdfundingbackend.enums.TransactionType
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.blockchain.pojo.GenerateProjectWalletRequest
import com.ampnet.crowdfundingbackend.blockchain.pojo.TransactionData
import com.ampnet.crowdfundingbackend.controller.pojo.response.PairWalletResponse
import com.ampnet.crowdfundingbackend.persistence.model.PairWalletCode
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

    @BeforeEach
    fun initTestData() {
        databaseCleanerService.deleteAllWalletsAndOwners()
        testData = TestData()
    }

    /* User Wallet */
    @Test
    fun mustBeAbleToGeneratePairWalletCode() {
        suppose("User did not create pair wallet code") {
            databaseCleanerService.deleteAllPairWalletCodes()
        }

        verify("User can generate pair wallet code") {
            val request = WalletCreateRequest(testData.address, testData.publicKey)
            val result = mockMvc.perform(
                    post("$walletPath/pair")
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk)
                    .andReturn()

            val pairWalletResponse: PairWalletResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(pairWalletResponse.code).isNotEmpty()
            assertThat(pairWalletResponse.address).isEqualTo(request.address)
            assertThat(pairWalletResponse.publicKey).isEqualTo(request.publicKey)
            testData.pairWalletCode = pairWalletResponse.code
        }
        verify("Pair wallet code is stored") {
            val optionalPairWalletCode = pairWalletCodeRepository.findByAddress(testData.address)
            assertThat(optionalPairWalletCode).isPresent
            val pairWalletCode = optionalPairWalletCode.get()
            assertThat(pairWalletCode.code).isEqualTo(testData.pairWalletCode)
            assertThat(pairWalletCode.address).isEqualTo(testData.address)
            assertThat(pairWalletCode.publicKey).isEqualTo(testData.publicKey)
            assertThat(pairWalletCode.createdAt).isBefore(ZonedDateTime.now())
        }
    }

    @Test
    fun mustReturnNotFoundForNonExistingPairWalletCode() {
        suppose("Pair wallet is missing") {
            databaseCleanerService.deleteAllPairWalletCodes()
        }

        verify("User will get not found for non existing pair wallet code") {
            mockMvc.perform(get("$walletPath/pair/000000"))
                    .andExpect(status().isNotFound)
        }
    }

    @Test
    fun mustReturnPairWalletCode() {
        suppose("User did create pair wallet code") {
            databaseCleanerService.deleteAllPairWalletCodes()
            testData.pairWalletCode = "N4CD12"
            val pairWalletCode = PairWalletCode(0, testData.address, testData.publicKey, testData.pairWalletCode,
                    ZonedDateTime.now())
            pairWalletCodeRepository.save(pairWalletCode)
        }

        verify("User can pair wallet code") {
            val result = mockMvc.perform(get("$walletPath/pair/${testData.pairWalletCode}"))
                    .andExpect(status().isOk)
                    .andReturn()

            val pairWalletResponse: PairWalletResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(pairWalletResponse.code).isEqualTo(testData.pairWalletCode)
            assertThat(pairWalletResponse.address).isEqualTo(testData.address)
            assertThat(pairWalletResponse.publicKey).isEqualTo(testData.publicKey)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGetOwnWallet() {
        suppose("User wallet exists") {
            testData.wallet = createWalletForUser(userUuid, testData.hash)
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
    @WithMockCrowdfoundUser
    fun mustReturnNotFoundForMissingWallet() {
        verify("Controller returns 404 for missing wallet") {
            mockMvc.perform(get(walletPath))
                    .andExpect(status().isNotFound)
        }
    }

    @Test
    @WithMockCrowdfoundUser
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
            val userWallet = userWalletRepository.findByUserUuid(userUuid)
            assertThat(userWallet).isPresent
            assertThat(userWallet.get().wallet.hash).isEqualTo(testData.hash)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustNotBeAbleToCreateAdditionalWallet() {
        suppose("User wallet exists") {
            testData.wallet = createWalletForUser(userUuid, testData.address)
        }

        verify("User cannot create a wallet") {
            val request = WalletCreateRequest(testData.address, testData.publicKey)
            mockMvc.perform(
                    post(walletPath)
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest)
        }
    }

    @Test
    @WithMockCrowdfoundUser
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

    /* Project Wallet */
    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGetCreateProjectWalletTransaction() {
        suppose("Project exists") {
            testData.organization = createOrganization("Org test", userUuid)
            testData.project = createProject("Test project", testData.organization, userUuid)
        }
        suppose("User has a wallet") {
            testData.wallet = createWalletForUser(userUuid, testData.hash)
        }
        suppose("Organization has a wallet") {
            createWalletForOrganization(testData.organization, testData.hash2)
        }
        suppose("Blockchain service successfully generates transaction to create project wallet") {
            val orgWalletHash = getWalletHash(testData.project.organization.wallet)
            val userWalletHash = getUserWalletHash(userUuid)
            testData.transactionData = generateTransactionData(testData.signedTransaction)
            val request = GenerateProjectWalletRequest(testData.project, orgWalletHash, userWalletHash)
            Mockito.`when`(
                    blockchainService.generateProjectWalletTransaction(request)
            ).thenReturn(testData.transactionData)
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
    @WithMockCrowdfoundUser
    fun mustNotBeAbleToGetCreateProjectWalletTransactionIfWalletExits() {
        suppose("Project exists") {
            val organization = createOrganization("Org test", userUuid)
            testData.project = createProject("Test project", organization, userUuid)
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
    @WithMockCrowdfoundUser
    fun mustThrowExceptionIfUserTriesToGenerateProjectWalletForNonExistingProject() {
        verify("System will throw error for missing project") {
            val response = mockMvc.perform(
                    get("$projectWalletPath/0/transaction"))
                    .andExpect(status().isBadRequest)
                    .andReturn()
            verifyResponseErrorCode(response, ErrorCode.PRJ_MISSING)
        }
    }

    /* Organization Wallet */
    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGetOrganizationWallet() {
        suppose("Organization exists") {
            testData.organization = createOrganization("Org test", userUuid)
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
    @WithMockCrowdfoundUser
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
    @WithMockCrowdfoundUser
    fun mustReturnNotFoundForMissingOrganizationWallet() {
        suppose("Organization exists") {
            testData.organization = createOrganization("Turk org", userUuid)
        }

        verify("Controller will return not found") {
            mockMvc.perform(get("$organizationWalletPath/${testData.organization.id}"))
                    .andExpect(status().isNotFound)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGetCreateOrganizationWallet() {
        suppose("User wallet exists") {
            testData.wallet = createWalletForUser(userUuid, testData.address)
        }
        suppose("Organization exists") {
            testData.organization = createOrganization("Turk org", userUuid)
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
    @WithMockCrowdfoundUser
    fun mustThrowErrorIfOrganizationIsMissingForCreateOrganizationWallet() {
        verify("System will throw error for missing organization") {
            val response = mockMvc.perform(
                    get("$organizationWalletPath/0/transaction"))
                    .andExpect(status().isBadRequest)
                    .andReturn()
            verifyResponseErrorCode(response, ErrorCode.ORG_MISSING)
        }
    }

    private class TestData {
        lateinit var wallet: Wallet
        lateinit var project: Project
        lateinit var transactionData: TransactionData
        lateinit var organization: Organization
        var walletId = -1
        var address = "0x14bC6a8219c798394726f8e86E040A878da1d99D"
        var hash = "0x4e4ee58ff3a9e9e78c2dfdbac0d1518e4e1039f9189267e1dc8d3e35cbdf7892"
        var hash2 = "0x4e4ee58ff3a9e9e78c2dfdbac0d1518e4e1039f9189267e1dc8d3e35cbdf7893"
        val publicKey = "0xC2D7CF95645D33006175B78989035C7c9061d3F9"
        var balance: Long = -1
        val signedTransaction = "SignedTransaction"
        lateinit var pairWalletCode: String
    }
}
