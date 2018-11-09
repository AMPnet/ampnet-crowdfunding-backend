package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.request.WalletDepositRequest
import com.ampnet.crowdfundingbackend.controller.pojo.response.TransactionResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.WalletResponse
import com.ampnet.crowdfundingbackend.enums.UserRoleType
import com.ampnet.crowdfundingbackend.persistence.model.AuthMethod
import com.ampnet.crowdfundingbackend.persistence.model.Currency
import com.ampnet.crowdfundingbackend.persistence.model.Transaction
import com.ampnet.crowdfundingbackend.persistence.model.TransactionType
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.persistence.model.Wallet
import com.ampnet.crowdfundingbackend.persistence.repository.RoleDao
import com.ampnet.crowdfundingbackend.persistence.repository.UserDao
import com.ampnet.crowdfundingbackend.persistence.repository.WalletDao
import com.ampnet.crowdfundingbackend.security.WithMockCrowdfoundUser
import com.ampnet.crowdfundingbackend.service.WalletService
import com.ampnet.crowdfundingbackend.service.pojo.DepositRequest
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.ZonedDateTime

class WalletControllerTest : ControllerTestBase() {

    private val myWalletPath = "/wallet"
    private val createWalletPath = "/wallet/create"
    private val depositWalletPath = "/wallet/deposit"

    @Autowired
    private lateinit var walletService: WalletService
    @Autowired
    private lateinit var userDao: UserDao
    @Autowired
    private lateinit var roleDao: RoleDao
    @Autowired
    private lateinit var walletDao: WalletDao

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
        suppose("User wallet exists with one transaction") {
            databaseCleanerService.deleteAllWalletsAndTransactions()
            testData.wallet = createWalletForUser(user.id)
            testData.transaction = depositToWallet(testData.wallet)
        }

        verify("Controller returns user wallet response") {
            val result = mockMvc.perform(get(myWalletPath))
                    .andExpect(status().isOk)
                    .andReturn()

            val walletResponse: WalletResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(walletResponse.id).isEqualTo(testData.wallet.id)
            assertThat(walletResponse.currency).isEqualTo(testData.wallet.currency.name)
            assertThat(walletResponse.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(walletResponse.transactions).hasSize(1)

            val transactionResponse = walletResponse.transactions[0]
            assertThat(transactionResponse.id).isEqualTo(testData.transaction.id)
            assertThat(transactionResponse.currency).isEqualTo(testData.transaction.currency.name)
            assertThat(transactionResponse.amount).isEqualTo(testData.transaction.amount)
            assertThat(transactionResponse.receiver).isEqualTo(testData.transaction.receiver)
            assertThat(transactionResponse.sender).isEqualTo(testData.transaction.sender)
            assertThat(transactionResponse.txHash).isEqualTo(testData.transaction.txHash)
            assertThat(transactionResponse.timestamp).isEqualTo(testData.transaction.timestamp)

            // TODO: change balance, mock fetching from blockchain
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

    @Test
    @WithMockCrowdfoundUser(email = "test@test.com")
    fun mustBeAbleToCreateWallet() {
        suppose("User does not have a wallet") {
            databaseCleanerService.deleteAllWalletsAndTransactions()
            user.id
        }

        verify("User can create a wallet") {
            val result = mockMvc.perform(post(createWalletPath))
                    .andExpect(status().isOk)
                    .andReturn()

            val walletResponse: WalletResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(walletResponse.id).isNotNull()
            assertThat(walletResponse.currency).isEqualTo(Currency.EUR.name) // default currency is eur
            assertThat(walletResponse.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(walletResponse.transactions).hasSize(0)

            testData.walletId = walletResponse.id
        }

        verify("Wallet is created") {
            val wallet = walletDao.findByOwnerId(user.id)
            assertThat(wallet).isPresent
            assertThat(wallet.get().id).isEqualTo(testData.walletId)
        }
    }

    @Test
    @WithMockCrowdfoundUser(email = "test@test.com")
    fun mustNotBeAbleToCreateAdditionalWallet() {
        suppose("User wallet exists") {
            databaseCleanerService.deleteAllWalletsAndTransactions()
            testData.wallet = createWalletForUser(user.id)
        }

        verify("User cannot create a wallet") {
            mockMvc.perform(post(createWalletPath))
                    .andExpect(status().isBadRequest)
                    .andReturn()
        }
    }

    @Test
    @WithMockCrowdfoundUser("test@test.com")
    fun mustBeAbleToDepositToOwnWallet() {
        suppose("User wallet exists") {
            databaseCleanerService.deleteAllWalletsAndTransactions()
            testData.wallet = createWalletForUser(user.id)
        }

        verify("User can deposit to own wallet") {
            val request = WalletDepositRequest(BigDecimal("6.66"), "electro")
            val result = mockMvc.perform(post(depositWalletPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk)
                    .andReturn()

            val transactionResponse: TransactionResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(transactionResponse.type).isEqualTo(TransactionType.DEPOSIT.name)
            assertThat(transactionResponse.id).isNotNull()
            assertThat(transactionResponse.sender).isEqualTo(request.sender)
            assertThat(transactionResponse.amount).isEqualTo(request.amount)
            assertThat(transactionResponse.currency).isEqualTo(Currency.EUR.name)
            assertThat(transactionResponse.timestamp).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(transactionResponse.txHash).isNotBlank()

            testData.transactionId = transactionResponse.id
        }
        verify("Transaction is stored in database") {
            val wallet = walletService.getWalletWithTransactionsForUser(user.id)
            assertThat(wallet).isNotNull
            assertThat(wallet!!.transactions).hasSize(1)
            assertThat(wallet.transactions[0].id).isEqualTo(testData.transactionId)
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

    private fun depositToWallet(wallet: Wallet): Transaction {
        val amount = BigDecimal("6.66")
        val sender = "sender"
        val txHash = "tx_hash"
        val currency = Currency.EUR
        val depositRequest = DepositRequest(wallet, amount, currency, sender, txHash)
        return walletService.depositToWallet(depositRequest)
    }

    private class TestData {
        lateinit var wallet: Wallet
        lateinit var transaction: Transaction
        var walletId = -1
        var transactionId = -1
    }
}
