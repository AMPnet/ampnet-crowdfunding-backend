package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.enums.TransactionType
import com.ampnet.crowdfundingbackend.persistence.model.Transaction
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.service.impl.TransactionServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TransactionServiceTest : JpaServiceTestBase() {

    private val transactionService: TransactionService by lazy { TransactionServiceImpl(transactionRepository) }
    private val user: User by lazy {
        databaseCleanerService.deleteAllUsers()
        createUser("admin@test.com", "Admin", "User")
    }

    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        testContext = TestContext()
    }

    @Test
    fun mustCreateOrgTransaction() {
        suppose("Service can create org transaction") {
            testContext.transaction = transactionService.createOrgTransaction(testContext.orgName, user.id)
        }

        verify("Org transaction is created") {
            val optionalTx = transactionRepository.findById(testContext.transaction.id)
            assertThat(optionalTx).isPresent
            val tx = optionalTx.get()
            assertThat(tx.type).isEqualTo(TransactionType.CREATE_ORG)
            assertThat(tx.userId).isEqualTo(user.id)
            assertThat(tx.description).contains(testContext.orgName)
        }
    }

    @Test
    fun mustCreateProjectTransaction() {
        suppose("Service can create project transaction") {
            testContext.transaction = transactionService.createProjectTransaction(testContext.projectName, user.id)
        }

        verify("Project transaction is created") {
            val optionalTx = transactionRepository.findById(testContext.transaction.id)
            assertThat(optionalTx).isPresent
            val tx = optionalTx.get()
            assertThat(tx.type).isEqualTo(TransactionType.CREATE_PROJECT)
            assertThat(tx.userId).isEqualTo(user.id)
            assertThat(tx.description).contains(testContext.projectName)
        }
    }

    @Test
    fun mustCreateInvestAllowanceTransaction() {
        suppose("Service can create invest allowance transaction") {
            testContext.transaction = transactionService.createInvestAllowanceTransaction(
                    testContext.projectName, testContext.amount, user.id)
        }

        verify("Invest allowance transaction is created") {
            val optionalTx = transactionRepository.findById(testContext.transaction.id)
            assertThat(optionalTx).isPresent
            val tx = optionalTx.get()
            assertThat(tx.type).isEqualTo(TransactionType.INVEST_ALLOWANCE)
            assertThat(tx.userId).isEqualTo(user.id)
            assertThat(tx.description).contains(testContext.projectName)
            assertThat(tx.description).contains("100.23")
        }
    }

    @Test
    fun mustCreateInvestTransaction() {
        suppose("Service can create invest transaction") {
            testContext.transaction = transactionService.createInvestTransaction(testContext.projectName, user.id)
        }

        verify("Invest transaction is created") {
            val optionalTx = transactionRepository.findById(testContext.transaction.id)
            assertThat(optionalTx).isPresent
            val tx = optionalTx.get()
            assertThat(tx.type).isEqualTo(TransactionType.INVEST)
            assertThat(tx.userId).isEqualTo(user.id)
            assertThat(tx.description).contains(testContext.projectName)
        }
    }

    private class TestContext {
        val orgName = "Org"
        val projectName = "Das project"
        val amount = 100_23L
        lateinit var transaction: Transaction
    }
}
