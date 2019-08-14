package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.blockchain.pojo.TransactionData
import com.ampnet.crowdfundingbackend.controller.pojo.request.AmountRequest
import com.ampnet.crowdfundingbackend.controller.pojo.response.DepositResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.DepositWithUserListResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.DepositWithUserResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.TransactionResponse
import com.ampnet.crowdfundingbackend.enums.PrivilegeType
import com.ampnet.crowdfundingbackend.enums.TransactionType
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.persistence.model.Deposit
import com.ampnet.crowdfundingbackend.security.WithMockCrowdfoundUser
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.fileUpload
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.ZonedDateTime
import java.util.UUID

class DepositControllerTest : ControllerTestBase() {

    private val depositPath = "/api/v1/deposit"
    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllDeposits()
        testContext = TestContext()
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToCreateDeposit() {
        suppose("User has a wallet") {
            databaseCleanerService.deleteAllWalletsAndOwners()
            createWalletForUser(userUuid, testContext.walletHash)
        }
        suppose("User has approved deposit") {
            createApprovedDeposit(userUuid, "tx_hash")
        }

        verify("User can create deposit") {
            val request = AmountRequest(testContext.amount)
            val result = mockMvc.perform(
                    post(depositPath)
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andExpect(MockMvcResultMatchers.status().isOk)
                    .andReturn()

            val deposit: DepositResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(deposit.user).isEqualTo(userUuid)
            assertThat(deposit.amount).isEqualTo(testContext.amount)
            assertThat(deposit.approved).isFalse()
            assertThat(deposit.reference).isNotNull()
            assertThat(deposit.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
        }
        verify("Deposit is stored") {
            val deposits = depositRepository.findAll()
            assertThat(deposits).hasSize(2)
            val deposit = deposits.first { it.approved.not() }
            assertThat(deposit.userUuid).isEqualTo(userUuid)
            assertThat(deposit.approved).isFalse()
            assertThat(deposit.reference).isNotNull()
            assertThat(deposit.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
        }
        verify("Mail notification is sent") {
            Mockito.verify(mailService, Mockito.times(1))
                    .sendDepositRequest(userUuid, testContext.amount)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustNotBeAbleToCreateDepositWithoutWallet() {
        suppose("User does not has a wallet") {
            databaseCleanerService.deleteAllWalletsAndOwners()
        }

        verify("User can create deposit") {
            val request = AmountRequest(testContext.amount)
            val result = mockMvc.perform(
                    post(depositPath)
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest)
                    .andReturn()
            verifyResponseErrorCode(result, ErrorCode.WALLET_MISSING)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGetPendingDeposit() {
        suppose("Deposit exists") {
            val deposit = createUnapprovedDeposit(userUuid)
            testContext.deposits = listOf(deposit)
        }

        verify("User can get pending deposit") {
            val result = mockMvc.perform(get(depositPath))
                    .andExpect(MockMvcResultMatchers.status().isOk)
                    .andReturn()

            val deposit: DepositResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(deposit.user).isEqualTo(userUuid)
            val savedDeposit = testContext.deposits.first()
            assertThat(deposit.id).isEqualTo(savedDeposit.id)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustGetNotFoundForNoPendingDeposit() {
        verify("User can get pending deposit") {
            mockMvc.perform(get(depositPath))
                    .andExpect(MockMvcResultMatchers.status().isNotFound)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_DEPOSIT])
    fun mustBeAbleToSearchByReference() {
        suppose("Deposit exists") {
            val deposit = createUnapprovedDeposit(userUuid)
            testContext.deposits = listOf(deposit)
        }
        suppose("User service will return user") {
            Mockito.`when`(userService.getUsers(listOf(userUuid))).thenReturn(listOf(createUserResponse(userUuid)))
        }

        verify("Admin can search deposit by reference") {
            val savedDeposit = testContext.deposits.first()
            val result = mockMvc.perform(
                    get("$depositPath/search").param("reference", savedDeposit.reference))
                    .andExpect(MockMvcResultMatchers.status().isOk)
                    .andReturn()

            val deposit: DepositWithUserResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(deposit.reference).isEqualTo(savedDeposit.reference)
            assertThat(deposit.user).isNotNull
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_DEPOSIT])
    fun mustNotBeAbleToFindByNonExistingReference() {
        suppose("Deposit exists") {
            val deposit = createUnapprovedDeposit(userUuid)
            testContext.deposits = listOf(deposit)
        }

        verify("Admin cannot search non-existing deposit by reference") {
            mockMvc.perform(
                    get("$depositPath/search").param("reference", "non-existing"))
                    .andExpect(MockMvcResultMatchers.status().isNotFound)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PWA_DEPOSIT])
    fun mustBeAbleToDeleteDeposit() {
        suppose("Deposit exists") {
            val deposit = createUnapprovedDeposit(userUuid)
            testContext.deposits = listOf(deposit)
        }

        verify("Admin can delete deposit") {
            mockMvc.perform(
                    delete("$depositPath/${testContext.deposits.first().id}"))
                    .andExpect(MockMvcResultMatchers.status().isOk)
        }
        verify("Deposit is deleted") {
            val optionalDeposit = depositRepository.findById(testContext.deposits.first().id)
            assertThat(optionalDeposit).isNotPresent
        }
        verify("Mail notification is sent") {
            Mockito.verify(mailService, Mockito.times(1))
                    .sendDepositInfo(userUuid, false)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustNotBeAbleToDeleteWithoutAdminPrivileges() {
        suppose("Deposit exists") {
            val deposit = createUnapprovedDeposit(userUuid)
            testContext.deposits = listOf(deposit)
        }

        verify("User without admin role cannot delete deposit") {
            mockMvc.perform(
                    delete("$depositPath/${testContext.deposits.first().id}"))
                    .andExpect(MockMvcResultMatchers.status().isForbidden)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PWA_DEPOSIT])
    fun mustBeAbleToApproveDeposit() {
        suppose("Deposit exists") {
            val deposit = createUnapprovedDeposit(userUuid)
            testContext.deposits = listOf(deposit)
        }
        suppose("File storage will store document") {
            testContext.multipartFile = MockMultipartFile("file", "test.txt",
                    "text/plain", "Some document data".toByteArray())
            Mockito.`when`(
                    cloudStorageService.saveFile(testContext.multipartFile.originalFilename,
                            testContext.multipartFile.bytes)
            ).thenReturn(testContext.documentLink)
        }

        verify("Admin can approve deposit") {
            val depositId = testContext.deposits.first().id
            val result = mockMvc.perform(
                    fileUpload("$depositPath/$depositId/approve?amount=${testContext.amount}")
                            .file(testContext.multipartFile))
                    .andExpect(MockMvcResultMatchers.status().isOk)
                    .andReturn()

            val depositResponse: DepositResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(depositResponse.id).isEqualTo(depositId)
            assertThat(depositResponse.approved).isEqualTo(true)
            assertThat(depositResponse.documentResponse?.link).isEqualTo(testContext.documentLink)
        }
        verify("Deposit is approved") {
            val optionalDeposit = depositRepository.findById(testContext.deposits.first().id)
            assertThat(optionalDeposit).isPresent
            val approvedDeposit = optionalDeposit.get()
            assertThat(approvedDeposit.approved).isTrue()
            assertThat(approvedDeposit.amount).isEqualTo(testContext.amount)
            assertThat(approvedDeposit.approvedByUserUuid).isEqualTo(userUuid)
            assertThat(approvedDeposit.approvedAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(approvedDeposit.document?.link).isEqualTo(testContext.documentLink)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PWA_DEPOSIT])
    fun mustNotBeAbleToApproveNonExistingDeposit() {
        suppose("File storage will store document") {
            testContext.multipartFile = MockMultipartFile("file", "test.txt",
                    "text/plain", "Some document data".toByteArray())
            Mockito.`when`(
                    cloudStorageService.saveFile(testContext.multipartFile.originalFilename,
                            testContext.multipartFile.bytes)
            ).thenReturn(testContext.documentLink)
        }

        verify("Admin can not approve non-existing deposit") {
            val result = mockMvc.perform(
                    fileUpload("$depositPath/0/approve")
                            .file(testContext.multipartFile)
                            .param("amount", testContext.amount.toString()))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest)
                    .andReturn()

            verifyResponseErrorCode(result, ErrorCode.WALLET_DEPOSIT_MISSING)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_DEPOSIT])
    fun mustBeAbleToGetUnapprovedDeposits() {
        suppose("Approved and unapproved deposits exist") {
            val unapproved = createUnapprovedDeposit(userUuid)
            val approved = createApprovedDeposit(userUuid)
            testContext.deposits = listOf(unapproved, approved)
        }
        suppose("User service will return user") {
            Mockito.`when`(userService.getUsers(listOf(userUuid))).thenReturn(listOf(createUserResponse(userUuid)))
        }

        verify("User can get unapproved deposit") {
            val result = mockMvc.perform(get("$depositPath/unapproved"))
                    .andExpect(MockMvcResultMatchers.status().isOk)
                    .andReturn()

            val deposits: DepositWithUserListResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(deposits.deposits).hasSize(1)
            val deposit = deposits.deposits[0]
            assertThat(deposit.approved).isFalse()
            assertThat(deposit.user).isNotNull
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_DEPOSIT])
    fun mustBeAbleToGetApprovedDeposits() {
        suppose("Approved and unapproved deposits exist") {
            val unapproved = createUnapprovedDeposit(userUuid)
            val approved = createApprovedDeposit(userUuid)
            testContext.deposits = listOf(unapproved, approved)
        }
        suppose("User service will return user") {
            Mockito.`when`(userService.getUsers(listOf(userUuid))).thenReturn(listOf(createUserResponse(userUuid)))
        }

        verify("User can get approved deposit") {
            val result = mockMvc.perform(get("$depositPath/approved"))
                    .andExpect(MockMvcResultMatchers.status().isOk)
                    .andReturn()

            val deposits: DepositWithUserListResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(deposits.deposits).hasSize(1)
            val deposit = deposits.deposits[0]
            assertThat(deposit.approved).isTrue()
            assertThat(deposit.user).isNotNull
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PWA_DEPOSIT])
    fun mustBeAbleToGenerateMintTransaction() {
        suppose("User has a wallet") {
            databaseCleanerService.deleteAllWalletsAndOwners()
            createWalletForUser(userUuid, testContext.walletHash)
        }
        suppose("Transaction info is clean") {
            databaseCleanerService.deleteAllTransactionInfo()
        }
        suppose("Approved deposit exists") {
            val approved = createApprovedDeposit(userUuid, amount = testContext.amount)
            testContext.deposits = listOf(approved)
        }
        suppose("Blockchain service will return tx") {
            testContext.transactionData = generateTransactionData("signed-transaction")
            Mockito.`when`(blockchainService.generateMintTransaction(
                    "0x43b0d9b605e68a0c50dc436757a86c82d97787cc", testContext.walletHash, testContext.amount)
            ).thenReturn(testContext.transactionData)
        }

        verify("Admin can generate mint transaction") {
            val depositId = testContext.deposits.first().id
            val result = mockMvc.perform(
                    post("$depositPath/$depositId/transaction"))
                    .andExpect(MockMvcResultMatchers.status().isOk)
                    .andReturn()

            val transactionResponse: TransactionResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(transactionResponse.tx).isEqualTo(testContext.transactionData)
            assertThat(transactionResponse.txId).isNotNull()
            assertThat(transactionResponse.info.txType).isEqualTo(TransactionType.MINT)
        }
        verify("Transaction info is created") {
            val transactionInfos = transactionInfoRepository.findAll()
            assertThat(transactionInfos).hasSize(1)
            val transactionInfo = transactionInfos[0]
            assertThat(transactionInfo.companionId).isEqualTo(testContext.deposits.first().id)
            assertThat(transactionInfo.type).isEqualTo(TransactionType.MINT)
            assertThat(transactionInfo.userUuid).isEqualTo(userUuid)
        }
    }

    private fun createUnapprovedDeposit(user: UUID): Deposit {
        val deposit = Deposit(0, user, "S34SDGFT", false, 10_000,
                null, null, null, null, ZonedDateTime.now())
        return depositRepository.save(deposit)
    }

    private class TestContext {
        val amount = 30_000L
        val walletHash = "0xa2addee8b62501fb423c8e69a6867a02eaa021a16f66583050a5dd643ad7e41b"
        var deposits = listOf<Deposit>()
        val documentLink = "document-link"
        lateinit var multipartFile: MockMultipartFile
        lateinit var transactionData: TransactionData
    }
}
