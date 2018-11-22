package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.TestBase
import com.ampnet.crowdfundingbackend.config.DatabaseCleanerService
import com.ampnet.crowdfundingbackend.config.PasswordEncoderConfig
import com.ampnet.crowdfundingbackend.enums.UserRoleType
import com.ampnet.crowdfundingbackend.exception.ResourceAlreadyExistsException
import com.ampnet.crowdfundingbackend.persistence.model.AuthMethod
import com.ampnet.crowdfundingbackend.persistence.model.Currency
import com.ampnet.crowdfundingbackend.persistence.model.Transaction
import com.ampnet.crowdfundingbackend.persistence.model.TransactionType
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.persistence.model.Wallet
import com.ampnet.crowdfundingbackend.persistence.repository.CountryDao
import com.ampnet.crowdfundingbackend.persistence.repository.MailTokenDao
import com.ampnet.crowdfundingbackend.persistence.repository.RoleDao
import com.ampnet.crowdfundingbackend.persistence.repository.TransactionDao
import com.ampnet.crowdfundingbackend.persistence.repository.UserDao
import com.ampnet.crowdfundingbackend.persistence.repository.WalletDao
import com.ampnet.crowdfundingbackend.service.impl.UserServiceImpl
import com.ampnet.crowdfundingbackend.service.impl.WalletServiceImpl
import com.ampnet.crowdfundingbackend.service.pojo.DepositRequest
import com.ampnet.crowdfundingbackend.service.pojo.TransferRequest
import com.ampnet.crowdfundingbackend.service.pojo.WithdrawRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.ZonedDateTime

@RunWith(SpringRunner::class)
@DataJpaTest
@Transactional(propagation = Propagation.SUPPORTS)
@Import(DatabaseCleanerService::class, PasswordEncoderConfig::class)
class WalletServiceTest : TestBase() {

    @Autowired
    private lateinit var databaseCleanerService: DatabaseCleanerService
    @Autowired
    private lateinit var walletDao: WalletDao
    @Autowired
    private lateinit var transactionDao: TransactionDao
    @Autowired
    private lateinit var roleDao: RoleDao
    @Autowired
    private lateinit var userDao: UserDao
    @Autowired
    private lateinit var countryDao: CountryDao
    @Autowired
    private lateinit var mailDao: MailTokenDao
    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    private val walletService: WalletService by lazy {
        val mailService = Mockito.mock(MailService::class.java)
        val userService = UserServiceImpl(userDao, roleDao, countryDao, mailDao, mailService, passwordEncoder)
        WalletServiceImpl(walletDao, transactionDao, userService)
    }

    private lateinit var testData: TestData
    private val user: User by lazy {
        databaseCleanerService.deleteAllUsers()
        createUser("test@email.com", "First", "Last")
    }

    @Before
    fun init() {
        testData = TestData()
    }

    @Test
    fun mustBeAbleToGetWalletForUserId() {
        suppose("User has a wallet") {
            databaseCleanerService.deleteAllWalletsAndTransactions()
            createWalletForUser(user.id)
        }

        verify("Service must fetch wallet for user with id") {
            val wallet = walletService.getWalletForUser(user.id)
            assertThat(wallet).isNotNull
            assertThat(wallet!!.ownerId).isEqualTo(user.id)
            assertThat(wallet.currency).isEqualTo(Currency.EUR)
        }
    }

    @Test
    fun mustBeAbleToCreateWalletForUser() {
        verify("Service can create wallet for a user") {
            databaseCleanerService.deleteAllWalletsAndTransactions()

            val wallet = walletService.createWallet(user.id)
            assertThat(wallet).isNotNull
            assertThat(wallet.ownerId).isEqualTo(user.id)
        }
    }

    @Test(expected = ResourceAlreadyExistsException::class)
    fun mustNotBeAbleToCreateMultipleWalletsForOneUser() {
        suppose("User has a wallet") {
            databaseCleanerService.deleteAllWalletsAndTransactions()
            createWalletForUser(user.id)
        }

        verify("Service cannot create additional account") {
            walletService.createWallet(user.id)
        }
    }

    @Test
    fun mustBeAbleToDepositToWallet() {
        suppose("User has a wallet") {
            databaseCleanerService.deleteAllWalletsAndTransactions()
            createWalletForUser(user.id)
        }

        verify("Service can record deposit transaction") {
            val userWallet = walletService.getWalletForUser(user.id)
            assertThat(userWallet).isNotNull
            val depositRequest = DepositRequest(
                    userWallet!!, testData.amount, testData.currency, testData.electro, testData.txHash)
            testData.transaction = walletService.depositToWallet(depositRequest)
            assertThat(testData.transaction.type).isEqualTo(TransactionType.DEPOSIT)
            assertThat(testData.transaction.txHash).isEqualTo(testData.txHash)
            assertThat(testData.transaction.sender).isEqualTo(testData.electro)
            assertThat(testData.transaction.receiver).isEqualTo(user.getFullName())
            assertThat(testData.transaction.amount).isEqualTo(testData.amount)
            assertThat(testData.transaction.currency).isEqualTo(testData.currency)
            assertThat(testData.transaction.walletId).isEqualTo(userWallet.id)
            assertThat(testData.transaction.timestamp).isBeforeOrEqualTo(ZonedDateTime.now())
        }
        verify("Verify transaction is stored in database") {
            val optionalStoredTransaction = transactionDao.findById(testData.transaction.id)
            assertThat(optionalStoredTransaction).isPresent
            assertThat(optionalStoredTransaction.get()).isEqualTo(testData.transaction)
        }
    }

    @Test
    fun mustBeAbleToWithdrawFromWallet() {
        suppose("User has a wallet") {
            databaseCleanerService.deleteAllWalletsAndTransactions()
            createWalletForUser(user.id)
        }

        verify("Service can record withdraw transaction") {
            val userWallet = walletService.getWalletForUser(user.id)
            assertThat(userWallet).isNotNull
            val withdrawRequest = WithdrawRequest(
                    userWallet!!, testData.amount, testData.currency, testData.electro, testData.txHash)
            testData.transaction = walletService.withdrawFromWallet(withdrawRequest)
            assertThat(testData.transaction.type).isEqualTo(TransactionType.WITHDRAW)
            assertThat(testData.transaction.txHash).isEqualTo(testData.txHash)
            assertThat(testData.transaction.receiver).isEqualTo(testData.electro)
            assertThat(testData.transaction.sender).isEqualTo(user.getFullName())
            assertThat(testData.transaction.amount).isEqualTo(testData.amount)
            assertThat(testData.transaction.currency).isEqualTo(testData.currency)
            assertThat(testData.transaction.walletId).isEqualTo(userWallet.id)
            assertThat(testData.transaction.timestamp).isBeforeOrEqualTo(ZonedDateTime.now())
        }
        verify("Verify transaction is stored in database") {
            val optionalWallet = walletService.getWalletWithTransactionsForUser(user.id)
            assertThat(optionalWallet).isNotNull
            assertThat(optionalWallet!!.transactions).hasSize(1)
            assertThat(optionalWallet.transactions[0]).isEqualTo(testData.transaction)
        }
    }

    @Test
    fun mustBeAbleToTransferFromWalletToWallet() {
        suppose("Two users have a wallet") {
            databaseCleanerService.deleteAllWalletsAndTransactions()
            createWalletForUser(user.id)
            testData.user2 = createUser("second@mail.com", "Second", "Name")
            createWalletForUser(testData.user2.id)
        }

        verify("Service can transfer founds from wallet to wallet") {
            val senderWallet = walletService.getWalletForUser(user.id)
            assertThat(senderWallet).isNotNull

            val transferRequest = TransferRequest(
                    user.id, testData.user2.id, testData.amount, testData.currency, testData.txHash)
            testData.transaction = walletService.transferFromWalletToWallet(transferRequest)
            assertThat(testData.transaction.type).isEqualTo(TransactionType.WITHDRAW)
            assertThat(testData.transaction.txHash).isEqualTo(testData.txHash)
            assertThat(testData.transaction.receiver).isEqualTo(testData.user2.getFullName())
            assertThat(testData.transaction.sender).isEqualTo(user.getFullName())
            assertThat(testData.transaction.amount).isEqualTo(testData.amount)
            assertThat(testData.transaction.currency).isEqualTo(testData.currency)
            assertThat(testData.transaction.walletId).isEqualTo(senderWallet!!.id)
            assertThat(testData.transaction.timestamp).isBeforeOrEqualTo(ZonedDateTime.now())
        }
        verify("Transaction is stored for sender") {
            val optionalStoredTransaction = transactionDao.findById(testData.transaction.id)
            assertThat(optionalStoredTransaction).isPresent
            assertThat(optionalStoredTransaction.get()).isEqualTo(testData.transaction)
        }
        verify("Transaction is stored for receiver") {
            val receiverWallet = walletService.getWalletWithTransactionsForUser(testData.user2.id)
            assertThat(receiverWallet).isNotNull
            assertThat(receiverWallet!!.transactions.size).isEqualTo(1)

            val transaction = receiverWallet.transactions[0]
            assertThat(transaction.type).isEqualTo(TransactionType.DEPOSIT)
            assertThat(transaction.txHash).isEqualTo(testData.txHash)
            assertThat(transaction.receiver).isEqualTo(testData.user2.getFullName())
            assertThat(transaction.sender).isEqualTo(user.getFullName())
            assertThat(transaction.amount).isEqualTo(testData.amount)
            assertThat(transaction.currency).isEqualTo(testData.currency)
            assertThat(transaction.walletId).isEqualTo(receiverWallet.id)
            assertThat(transaction.timestamp).isBeforeOrEqualTo(ZonedDateTime.now())
        }
    }

    private fun createUser(email: String, firstName: String, lastName: String): User {
        val user = User::class.java.newInstance()
        user.authMethod = AuthMethod.EMAIL
        user.createdAt = ZonedDateTime.now()
        user.email = email
        user.enabled = true
        user.firstName = firstName
        user.lastName = lastName
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
        val amount = BigDecimal("5.24")
        val currency = Currency.EUR
        val electro = "Electro"
        val txHash = "0x_Das_Hash"
        lateinit var transaction: Transaction
        lateinit var user2: User
    }
}
