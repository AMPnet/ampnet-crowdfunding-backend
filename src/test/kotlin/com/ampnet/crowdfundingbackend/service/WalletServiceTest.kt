package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.TestBase
import com.ampnet.crowdfundingbackend.config.DatabaseCleanerService
import com.ampnet.crowdfundingbackend.enums.UserRoleType
import com.ampnet.crowdfundingbackend.exception.ResourceAlreadyExistsException
import com.ampnet.crowdfundingbackend.persistence.model.AuthMethod
import com.ampnet.crowdfundingbackend.persistence.model.Currency
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.persistence.model.Wallet
import com.ampnet.crowdfundingbackend.persistence.repository.RoleDao
import com.ampnet.crowdfundingbackend.persistence.repository.TransactionDao
import com.ampnet.crowdfundingbackend.persistence.repository.WalletDao
import com.ampnet.crowdfundingbackend.service.impl.WalletServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.junit4.SpringRunner
import java.time.ZonedDateTime

@RunWith(SpringRunner::class)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class WalletServiceTest : TestBase() {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var walletDao: WalletDao

    @Autowired
    private lateinit var transactionDao: TransactionDao

    @Autowired
    private lateinit var roleDao: RoleDao

    private val user: User by lazy { createTestUser() }
    private val walletService: WalletService by lazy { WalletServiceImpl(walletDao, transactionDao) }
    private val databaseCleanerService: DatabaseCleanerService by lazy {
        DatabaseCleanerService(entityManager.entityManager)
    }

    @Test
    fun mustBeAbleToGetWalletForUserId() {
        suppose("User wallet exists in database") {
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

    private fun createTestUser(): User {
        val user = User::class.java.newInstance()
        user.authMethod = AuthMethod.EMAIL
        user.createdAt = ZonedDateTime.now()
        user.email = "test@email.com"
        user.enabled = true
        user.role = roleDao.getOne(UserRoleType.USER.id)
        return entityManager.persist(user)
    }

    private fun createWalletForUser(userId: Int): Wallet {
        val wallet = Wallet::class.java.newInstance()
        wallet.ownerId = userId
        wallet.currency = Currency.EUR
        wallet.transactions = emptyList()
        wallet.createdAt = ZonedDateTime.now()
        return entityManager.persist(wallet)
    }
}
