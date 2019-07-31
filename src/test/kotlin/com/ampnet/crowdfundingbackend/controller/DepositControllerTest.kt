package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.response.DepositResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.DepositWithUserListResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.DepositWithUserResponse
import com.ampnet.crowdfundingbackend.enums.PrivilegeType
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.persistence.model.Deposit
import com.ampnet.crowdfundingbackend.persistence.repository.DepositRepository
import com.ampnet.crowdfundingbackend.security.WithMockCrowdfoundUser
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockMultipartFile
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.fileUpload
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.ZonedDateTime
import java.util.UUID

class DepositControllerTest : ControllerTestBase() {

    @Autowired
    private lateinit var depositRepository: DepositRepository

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

        verify("User can create deposit") {
            val result = mockMvc.perform(post(depositPath))
                    .andExpect(MockMvcResultMatchers.status().isOk)
                    .andReturn()

            val deposit: DepositResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(deposit.user).isEqualTo(userUuid)
            assertThat(deposit.approved).isFalse()
            assertThat(deposit.reference).isNotNull()
            assertThat(deposit.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
        }
        verify("Deposit is stored") {
            val deposits = depositRepository.findAll()
            assertThat(deposits).hasSize(1)
            val deposit = deposits.first()
            assertThat(deposit.userUuid).isEqualTo(userUuid)
            assertThat(deposit.approved).isFalse()
            assertThat(deposit.reference).isNotNull()
            assertThat(deposit.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustNotBeAbleToCreateDepositWithoutWallet() {
        suppose("User does not has a wallet") {
            databaseCleanerService.deleteAllWalletsAndOwners()
        }

        verify("User can create deposit") {
            val result = mockMvc.perform(post(depositPath))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest)
                    .andReturn()
            verifyResponseErrorCode(result, ErrorCode.WALLET_MISSING)
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
                    get(depositPath).param("reference", savedDeposit.reference))
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
                    get(depositPath).param("reference", "non-existing"))
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

    private fun createUnapprovedDeposit(user: UUID): Deposit {
        val deposit = Deposit(0, user, "S34SDGFT", false,
                null, null, null, null, ZonedDateTime.now()
        )
        return depositRepository.save(deposit)
    }

    private fun createApprovedDeposit(user: UUID): Deposit {
        val document = saveDocument("doc", testContext.documentLink, "type", 1, user)
        val deposit = Deposit(0, user, "S34SDGFT", true,
                user, ZonedDateTime.now(), testContext.amount, document, ZonedDateTime.now()
        )
        return depositRepository.save(deposit)
    }

    private class TestContext {
        val amount = 30_000L
        val walletHash = "0xa2addee8b62501fb423c8e69a6867a02eaa021a16f66583050a5dd643ad7e41b"
        var deposits = listOf<Deposit>()
        val documentLink = "document-link"
        lateinit var multipartFile: MockMultipartFile
    }
}
