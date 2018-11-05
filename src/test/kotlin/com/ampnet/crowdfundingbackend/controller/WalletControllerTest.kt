package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.config.DatabaseCleanerService
import com.ampnet.crowdfundingbackend.controller.pojo.response.WalletResponse
import com.ampnet.crowdfundingbackend.enums.UserRoleType
import com.ampnet.crowdfundingbackend.persistence.model.AuthMethod
import com.ampnet.crowdfundingbackend.persistence.model.Currency
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.persistence.model.Wallet
import com.ampnet.crowdfundingbackend.persistence.repository.RoleDao
import com.ampnet.crowdfundingbackend.persistence.repository.UserDao
import com.ampnet.crowdfundingbackend.persistence.repository.WalletDao
import com.ampnet.crowdfundingbackend.security.WithMockCrowdfoundUser
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.ZonedDateTime

class WalletControllerTest : ControllerTestBase() {

    private val myWalletPath = "/wallet"

    @Autowired
    private lateinit var userDao: UserDao
    @Autowired
    private lateinit var roleDao: RoleDao
    @Autowired
    private lateinit var walletDao: WalletDao
    @Autowired
    private lateinit var databaseCleanerService: DatabaseCleanerService

    private lateinit var testData: TestData
    private val user: User by lazy {
        databaseCleanerService.deleteAllUsers()
        createUser("test@test.com")
    }

    @Before
    fun initTestData() {
        testData = TestData()
    }

    @Test
    @WithMockCrowdfoundUser(email = "test@test.com")
    fun mustBeAbleToGetOwnWallet() {
        suppose("User wallet exists") {
            databaseCleanerService.deleteAllWalletsAndTransactions()
            testData.wallet = createWalletForUser(user.id)
        }

        verify("Controller returns user wallet response") {
            val result = mockMvc.perform(get(myWalletPath))
                    .andExpect(status().isOk)
                    .andReturn()

            val walletResponse: WalletResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(walletResponse.id).isEqualTo(testData.wallet.id)
            assertThat(walletResponse.currency).isEqualTo(testData.wallet.currency.name)
            assertThat(walletResponse.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            // TODO: change balance
            assertThat(walletResponse.balance).isZero()
        }
    }

    @Test
    @WithMockCrowdfoundUser(email = "test@test.com")
    fun mustReturnNotFoundForMissingWallet() {
        suppose("User does not have a wallet") {
            databaseCleanerService.deleteAllWalletsAndTransactions()
        }

        verify("Controller returns 404 for missing wallet") {
            mockMvc.perform(get(myWalletPath))
                    .andExpect(status().isNotFound)
        }
    }

    private fun createUser(email: String): User {
        val user = User::class.java.newInstance()
        user.authMethod = AuthMethod.EMAIL
        user.createdAt = ZonedDateTime.now()
        user.email = email
        user.enabled = true
        user.firstName = "First"
        user.lastName = "Last"
        user.role = roleDao.getOne(UserRoleType.USER.id)
        return userDao.save(user)
    }

    private fun createWalletForUser(userId: Int): Wallet {
        val wallet = Wallet::class.java.newInstance()
        wallet.ownerId = userId
        wallet.currency = Currency.EUR
        wallet.transactions = emptyList()
        wallet.createdAt = ZonedDateTime.now()
        return walletDao.save(wallet)
    }

    private class TestData {
        lateinit var wallet: Wallet
    }
}
