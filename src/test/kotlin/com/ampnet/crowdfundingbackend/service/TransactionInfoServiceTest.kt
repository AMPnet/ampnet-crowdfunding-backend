package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.enums.TransactionType
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.TransactionInfo
import com.ampnet.crowdfundingbackend.service.impl.TransactionInfoServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TransactionInfoServiceTest : JpaServiceTestBase() {

    private val transactionInfoService: TransactionInfoService by lazy {
        TransactionInfoServiceImpl(transactionInfoRepository)
    }
    private val organization: Organization by lazy {
        databaseCleanerService.deleteAllOrganizations()
        createOrganization("Das Organ", userUuid)
    }
    private val project: Project by lazy {
        databaseCleanerService.deleteAllProjects()
        createProject("Projectos", organization, userUuid)
    }

    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        testContext = TestContext()
        databaseCleanerService.deleteAllTransactionInfo()
    }

    @Test
    fun mustCreateOrgTransaction() {
        suppose("Service can create org transactionInfo") {
            testContext.transactionInfo = transactionInfoService.createOrgTransaction(organization, userUuid)
        }

        verify("Org transactionInfo is created") {
            val optionalTx = transactionInfoRepository.findById(testContext.transactionInfo.id)
            assertThat(optionalTx).isPresent
            val tx = optionalTx.get()
            assertThat(tx.type).isEqualTo(TransactionType.CREATE_ORG)
            assertThat(tx.userUuid).isEqualTo(userUuid)
            assertThat(tx.description).contains(organization.name)
        }
    }

    @Test
    fun mustCreateProjectTransaction() {
        suppose("Service can create project transactionInfo") {
            testContext.transactionInfo = transactionInfoService.createProjectTransaction(project, userUuid)
        }

        verify("Project transactionInfo is created") {
            val optionalTx = transactionInfoRepository.findById(testContext.transactionInfo.id)
            assertThat(optionalTx).isPresent
            val tx = optionalTx.get()
            assertThat(tx.type).isEqualTo(TransactionType.CREATE_PROJECT)
            assertThat(tx.userUuid).isEqualTo(userUuid)
            assertThat(tx.description).contains(project.name)
        }
    }

    @Test
    fun mustCreateInvestAllowanceTransaction() {
        suppose("Service can create invest allowance transactionInfo") {
            testContext.transactionInfo = transactionInfoService.createInvestAllowanceTransaction(
                    project.name, testContext.amount, userUuid)
        }

        verify("Invest allowance transactionInfo is created") {
            val optionalTx = transactionInfoRepository.findById(testContext.transactionInfo.id)
            assertThat(optionalTx).isPresent
            val tx = optionalTx.get()
            assertThat(tx.type).isEqualTo(TransactionType.INVEST_ALLOWANCE)
            assertThat(tx.userUuid).isEqualTo(userUuid)
            assertThat(tx.description).contains(project.name)
            assertThat(tx.description).contains("100.23")
        }
    }

    @Test
    fun mustCreateInvestTransaction() {
        suppose("Service can create invest transactionInfo") {
            testContext.transactionInfo = transactionInfoService.createInvestTransaction(
                    project.name, userUuid)
        }

        verify("Invest transactionInfo is created") {
            val optionalTx = transactionInfoRepository.findById(testContext.transactionInfo.id)
            assertThat(optionalTx).isPresent
            val tx = optionalTx.get()
            assertThat(tx.type).isEqualTo(TransactionType.INVEST)
            assertThat(tx.userUuid).isEqualTo(userUuid)
            assertThat(tx.description).contains(project.name)
        }
    }

    private class TestContext {
        val amount = 100_23L
        lateinit var transactionInfo: TransactionInfo
    }
}
