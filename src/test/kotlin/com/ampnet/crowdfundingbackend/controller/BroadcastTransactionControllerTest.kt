package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.response.TxHashResponse
import com.ampnet.crowdfundingbackend.enums.Currency
import com.ampnet.crowdfundingbackend.enums.TransactionType
import com.ampnet.crowdfundingbackend.enums.WalletType
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.TransactionInfo
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.service.pojo.PostTransactionType
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.ZonedDateTime
import java.util.*

class BroadcastTransactionControllerTest : ControllerTestBase() {

    private val broadcastPath = "/tx_broadcast"
    private val txSignedParam = "tx_sig"
    private val txIdParam = "tx_id"

    private val signedTransaction = "SignedTransaction"
    private val txHash = "tx_hash"

    private lateinit var user: User
    private lateinit var organization: Organization
    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        testContext = TestContext()
        databaseCleanerService.deleteAllWalletsAndOwners()
        databaseCleanerService.deleteAllOrganizations()
        databaseCleanerService.deleteAllTransactionInfo()
        user = createUser(defaultEmail)
        organization = createOrganization("Turk org", userUuid)
    }

    @Test
    fun mustNotBeAbleToPostNonExistingTransaction() {
        verify("User cannot post signed non existing transaction") {
            val response = mockMvc.perform(
                    post(broadcastPath)
                            .param(txSignedParam, signedTransaction)
                            .param(txIdParam, "0"))
                    .andExpect(status().isBadRequest)
                    .andReturn()
            verifyResponseErrorCode(response, ErrorCode.TX_MISSING)
        }
    }

    @Test
    fun mustBeAbleToCreateOrganizationWallet() {
        suppose("TransactionInfo exists for create organization wallet") {
            testContext.transactionInfo = createTransactionInfo(TransactionType.CREATE_ORG, userUuid, organization.id)
        }
        suppose("Blockchain service successfully generates transaction to create organization wallet") {
            Mockito.`when`(
                    blockchainService.postTransaction(signedTransaction, PostTransactionType.ORG_CREATE)
            ).thenReturn(txHash)
        }

        verify("User can create organization wallet") {
            val result = mockMvc.perform(
                    post(broadcastPath)
                            .param(txSignedParam, signedTransaction)
                            .param(txIdParam, testContext.transactionInfo.id.toString()))
                    .andExpect(status().isOk)
                    .andReturn()

            val txHashResponse: TxHashResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(txHashResponse.txHash).isEqualTo(txHash)
        }
        verify("Organization wallet is created") {
            val optionalOrganization = organizationRepository.findById(organization.id)
            assertThat(optionalOrganization).isPresent
            val organizationWallet = optionalOrganization.get().wallet ?: fail("Wallet must not be null")
            assertThat(organizationWallet.id).isNotNull()
            assertThat(organizationWallet.hash).isEqualTo(txHash)
            assertThat(organizationWallet.currency).isEqualTo(Currency.EUR)
            assertThat(organizationWallet.type).isEqualTo(WalletType.ORG)
            assertThat(organizationWallet.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
        }
        verify("TransactionInfo is deleted") {
            val transactionInfo = transactionInfoRepository.findById(testContext.transactionInfo.id)
            assertThat(transactionInfo).isNotPresent
        }
    }

    @Test
    fun mustThrowErrorIfCompanionOrganizationIdIsMissing() {
        suppose("TransactionInfo exists for create organization wallet but without companion org id") {
            testContext.transactionInfo = createTransactionInfo(TransactionType.CREATE_ORG, userUuid)
        }

        verify("User can create organization wallet") {
            val result = mockMvc.perform(
                    post(broadcastPath)
                            .param(txSignedParam, signedTransaction)
                            .param(txIdParam, testContext.transactionInfo.id.toString()))
                    .andExpect(status().isBadRequest)
                    .andReturn()
            verifyResponseErrorCode(result, ErrorCode.TX_COMPANION_ID_MISSING)
        }
    }

    @Test
    fun mustThrowErrorIfOrganizationIsMissing() {
        suppose("TransactionInfo exists for create organization wallet but with non existing org id") {
            testContext.transactionInfo = createTransactionInfo(TransactionType.CREATE_ORG, userUuid, 0)
        }

        verify("User can create organization wallet") {
            val result = mockMvc.perform(
                    post(broadcastPath)
                            .param(txSignedParam, signedTransaction)
                            .param(txIdParam, testContext.transactionInfo.id.toString()))
                    .andExpect(status().isBadRequest)
                    .andReturn()
            verifyResponseErrorCode(result, ErrorCode.ORG_MISSING)
        }
    }

    @Test
    fun mustBeAbleToCreateProjectWalletWithTransaction() {
        suppose("Project exists") {
            databaseCleanerService.deleteAllProjects()
            testContext.project = createProject("Test project", organization, user)
        }
        suppose("TransactionInfo exists for create project wallet") {
            testContext.transactionInfo = createTransactionInfo(
                    TransactionType.CREATE_PROJECT, userUuid, testContext.project.id)
        }
        suppose("Blockchain service successfully adds project wallet") {
            Mockito.`when`(
                    blockchainService.postTransaction(signedTransaction, PostTransactionType.PRJ_CREATE)
            ).thenReturn(txHash)
        }

        verify("User can create project wallet") {
            val result = mockMvc.perform(
                    post(broadcastPath)
                            .param(txSignedParam, signedTransaction)
                            .param(txIdParam, testContext.transactionInfo.id.toString()))
                    .andExpect(status().isOk)
                    .andReturn()

            val txHashResponse: TxHashResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(txHashResponse.txHash).isEqualTo(txHash)
        }
        verify("Wallet is created") {
            val optionalProject = projectRepository.findByIdWithWallet(testContext.project.id)
            assertThat(optionalProject).isPresent
            val projectWallet = optionalProject.get().wallet ?: fail("Wallet must not be null")
            assertThat(projectWallet.id).isNotNull()
            assertThat(projectWallet.hash).isEqualTo(txHash)
            assertThat(projectWallet.currency).isEqualTo(Currency.EUR)
            assertThat(projectWallet.type).isEqualTo(WalletType.PROJECT)
            assertThat(projectWallet.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
        }
        verify("TransactionInfo is deleted") {
            val transactionInfo = transactionInfoRepository.findById(testContext.transactionInfo.id)
            assertThat(transactionInfo).isNotPresent
        }
    }

    @Test
    fun mustThrowErrorIfCompanionProjectIdIsMissing() {
        suppose("TransactionInfo exists for create project wallet but without companion project id") {
            testContext.transactionInfo = createTransactionInfo(TransactionType.CREATE_PROJECT, userUuid)
        }

        verify("User can create organization wallet") {
            val result = mockMvc.perform(
                    post(broadcastPath)
                            .param(txSignedParam, signedTransaction)
                            .param(txIdParam, testContext.transactionInfo.id.toString()))
                    .andExpect(status().isBadRequest)
                    .andReturn()
            verifyResponseErrorCode(result, ErrorCode.TX_COMPANION_ID_MISSING)
        }
    }

    @Test
    fun mustThrowErrorIfProjectIsMissing() {
        suppose("TransactionInfo exists for create project wallet but without companion project id") {
            testContext.transactionInfo = createTransactionInfo(TransactionType.CREATE_PROJECT, userUuid, 0)
        }

        verify("User can create organization wallet") {
            val result = mockMvc.perform(
                    post(broadcastPath)
                            .param(txSignedParam, signedTransaction)
                            .param(txIdParam, testContext.transactionInfo.id.toString()))
                    .andExpect(status().isBadRequest)
                    .andReturn()
            verifyResponseErrorCode(result, ErrorCode.PRJ_MISSING)
        }
    }

    @Test
    fun mustBeAbleToPostSignedInvestAllowanceTransaction() {
        suppose("Project exists") {
            databaseCleanerService.deleteAllProjects()
            testContext.project = createProject("Test project", organization, user)
        }
        suppose("TransactionInfo exists for invest allowance transaction") {
            testContext.transactionInfo = createTransactionInfo(TransactionType.INVEST_ALLOWANCE, userUuid)
        }
        suppose("Blockchain service will accept signed transaction for project investment") {
            Mockito.`when`(
                    blockchainService.postTransaction(signedTransaction, PostTransactionType.PRJ_INVEST)
            ).thenReturn(txHash)
        }

        verify("User can post signed transaction to invest in project") {
            val result = mockMvc.perform(
                    post(broadcastPath)
                            .param(txSignedParam, signedTransaction)
                            .param(txIdParam, testContext.transactionInfo.id.toString()))
                    .andExpect(status().isOk)
                    .andReturn()

            val txHashResponse: TxHashResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(txHashResponse.txHash).isEqualTo(txHash)
        }
        verify("TransactionInfo is deleted") {
            val transactionInfo = transactionInfoRepository.findById(testContext.transactionInfo.id)
            assertThat(transactionInfo).isNotPresent
        }
    }

    @Test
    fun mustBeAbleToPostSignedInvestTransaction() {
        suppose("Project exists") {
            databaseCleanerService.deleteAllProjects()
            testContext.project = createProject("Test project", organization, user)
        }
        suppose("TransactionInfo exists for invest transaction") {
            testContext.transactionInfo = createTransactionInfo(TransactionType.INVEST, userUuid)
        }
        suppose("Blockchain service will accept signed transaction for project investment confirmation") {
            Mockito.`when`(
                    blockchainService.postTransaction(signedTransaction, PostTransactionType.PRJ_INVEST_CONFIRM)
            ).thenReturn(txHash)
        }

        verify("User can post signed transaction to confirm investment in project") {
            val result = mockMvc.perform(
                    post(broadcastPath)
                            .param(txSignedParam, signedTransaction)
                            .param(txIdParam, testContext.transactionInfo.id.toString()))
                    .andExpect(status().isOk)
                    .andReturn()

            val txHashResponse: TxHashResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(txHashResponse.txHash).isEqualTo(txHash)
        }
        verify("TransactionInfo is deleted") {
            val transactionInfo = transactionInfoRepository.findById(testContext.transactionInfo.id)
            assertThat(transactionInfo).isNotPresent
        }
    }

    private fun createTransactionInfo(type: TransactionType, userUuid: String, companionId: Int? = null): TransactionInfo {
        val transactionInfo = TransactionInfo::class.java.getDeclaredConstructor().newInstance().apply {
            this.type = type
            this.title = "title"
            this.description = "description"
            this.userUuid = userUuid
            this.companionId = companionId
        }
        return transactionInfoRepository.save(transactionInfo)
    }

    private class TestContext {
        lateinit var transactionInfo: TransactionInfo
        lateinit var project: Project
    }
}
