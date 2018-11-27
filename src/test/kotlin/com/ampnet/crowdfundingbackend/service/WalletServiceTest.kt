package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.exception.ResourceAlreadyExistsException
import com.ampnet.crowdfundingbackend.persistence.model.Currency
import com.ampnet.crowdfundingbackend.persistence.model.Transaction
import com.ampnet.crowdfundingbackend.persistence.model.TransactionType
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.service.impl.UserServiceImpl
import com.ampnet.crowdfundingbackend.service.impl.WalletServiceImpl
import com.ampnet.crowdfundingbackend.service.pojo.DepositRequest
import com.ampnet.crowdfundingbackend.service.pojo.TransferRequest
import com.ampnet.crowdfundingbackend.service.pojo.WithdrawRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.math.BigDecimal
import java.time.ZonedDateTime

class WalletServiceTest : JpaServiceTestBase() {

    private val walletService: WalletService by lazy {
        val mailService = Mockito.mock(MailService::class.java)
        val userService = UserServiceImpl(userRepository, roleRepository, countryRepository, mailRepository,
                organizationInviteRepository, mailService, passwordEncoder)
        WalletServiceImpl(walletRepository, transactionRepository, userService)
    }

    private lateinit var testData: TestData
    private val user: User by lazy {
        databaseCleanerService.deleteAllUsers()
        createUser("test@email.com", "First", "Last")
    }

    @BeforeEach
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

    @Test
    fun mustNotBeAbleToCreateMultipleWalletsForOneUser() {
        suppose("User has a wallet") {
            databaseCleanerService.deleteAllWalletsAndTransactions()
            createWalletForUser(user.id)
        }

        verify("Service cannot create additional account") {
            assertThrows<ResourceAlreadyExistsException> { walletService.createWallet(user.id) }
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
            val optionalStoredTransaction = transactionRepository.findById(testData.transaction.id)
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
            val optionalStoredTransaction = transactionRepository.findById(testData.transaction.id)
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

    private class TestData {
        val amount = BigDecimal("5.24")
        val currency = Currency.EUR
        val electro = "Electro"
        val txHash = "0x_Das_Hash"
        lateinit var transaction: Transaction
        lateinit var user2: User
    }
}
