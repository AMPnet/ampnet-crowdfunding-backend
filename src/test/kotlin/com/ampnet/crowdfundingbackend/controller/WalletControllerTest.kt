package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.request.WalletCreateRequest
import com.ampnet.crowdfundingbackend.controller.pojo.response.WalletResponse
import com.ampnet.crowdfundingbackend.enums.Currency
import com.ampnet.crowdfundingbackend.enums.WalletType
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.persistence.model.Wallet
import com.ampnet.crowdfundingbackend.security.WithMockCrowdfoundUser
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.ZonedDateTime

class WalletControllerTest : ControllerTestBase() {

    private val walletPath = "/wallet"

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
        suppose("User wallet exists with one transaction") {
            testData.wallet = createWalletForUser(user, testData.address)
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

            // TODO: change balance, mock fetching from blockchain
            assertThat(walletResponse.balance).isZero()
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

    private class TestData {
        lateinit var wallet: Wallet
        var walletId = -1
        var address = "0x14bC6a8219c798394726f8e86E040A878da1d99D"
    }
}
