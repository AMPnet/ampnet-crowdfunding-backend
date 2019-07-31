package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.request.WithdrawApproveRequest
import com.ampnet.crowdfundingbackend.controller.pojo.request.WithdrawCreateRequest
import com.ampnet.crowdfundingbackend.controller.pojo.response.WithdrawResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.WithdrawWithUserAndAcceptanceListResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.WithdrawWithUserListResponse
import com.ampnet.crowdfundingbackend.enums.PrivilegeType
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.persistence.model.Withdraw
import com.ampnet.crowdfundingbackend.persistence.repository.WithdrawRepository
import com.ampnet.crowdfundingbackend.security.WithMockCrowdfoundUser
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.ZonedDateTime

class WithdrawControllerTest : ControllerTestBase() {

    @Autowired
    private lateinit var withdrawRepository: WithdrawRepository

    private val withdrawPath = "/api/v1/withdraw"
    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllWithdraws()
        testContext = TestContext()
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToCreateWithdraw() {
        verify("User can create Withdraw") {
            val request = WithdrawCreateRequest(testContext.amount)
            val result = mockMvc.perform(
                    post(withdrawPath)
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andExpect(status().isOk)
                    .andReturn()

            val withdrawResponse: WithdrawResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(withdrawResponse.user).isEqualTo(userUuid)
            assertThat(withdrawResponse.amount).isEqualTo(testContext.amount)
            assertThat(withdrawResponse.approved).isFalse()
            assertThat(withdrawResponse.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(withdrawResponse.approvedAt).isNull()
            assertThat(withdrawResponse.approvedBy).isNull()
            assertThat(withdrawResponse.approvedReference).isNull()
        }
        verify("Withdraw is created") {
            val withdraws = withdrawRepository.findAll()
            assertThat(withdraws).hasSize(1)
            val withdraw = withdraws[0]
            assertThat(withdraw.userUuid).isEqualTo(userUuid)
            assertThat(withdraw.amount).isEqualTo(testContext.amount)
            assertThat(withdraw.approved).isFalse()
            assertThat(withdraw.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(withdraw.approvedAt).isNull()
            assertThat(withdraw.approvedByUserUuid).isNull()
            assertThat(withdraw.approvedReference).isNull()
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_WITHDRAW])
    fun mustBeAbleToGetUnapprovedWithdraws() {
        suppose("Some withdraws are created") {
            val approvedWithdraw = Withdraw(0, userUuid, testContext.amount, true, "ref",
                    userUuid, ZonedDateTime.now(), ZonedDateTime.now())
            val unapprovedWithdraw = Withdraw(0, userUuid, testContext.amount, false, null,
                    null, null, ZonedDateTime.now())
            testContext.withdraws =
                    listOf(withdrawRepository.save(approvedWithdraw), withdrawRepository.save(unapprovedWithdraw))
        }
        suppose("User has a wallet") {
            databaseCleanerService.deleteAllWalletsAndOwners()
            createWalletForUser(userUuid, testContext.walletHash)
        }
        suppose("User service will return user data") {
            Mockito.`when`(userService.getUsers(listOf(userUuid))).thenReturn(listOf(createUserResponse(userUuid)))
        }

        verify("Admin can get list of unapproved withdraws") {
            val result = mockMvc.perform(get("$withdrawPath/unapproved"))
                    .andExpect(status().isOk)
                    .andReturn()

            val withdrawList: WithdrawWithUserListResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(withdrawList.withdraws).hasSize(1)
            val withdraw = withdrawList.withdraws[0]
            assertThat(withdraw.amount).isEqualTo(testContext.amount)
            assertThat(withdraw.approved).isFalse()
            assertThat(withdraw.user?.uuid).isEqualTo(userUuid)
            assertThat(withdraw.userWallet).isEqualTo(testContext.walletHash)
            assertThat(withdraw.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(withdraw.id).isNotNull()
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_WITHDRAW])
    fun mustBeAbleToGetApprovedWithdraws() {
        suppose("Some withdraws are created") {
            val approvedWithdraw = Withdraw(0, userUuid, testContext.amount, true, testContext.reference,
                    userUuid, ZonedDateTime.now(), ZonedDateTime.now())
            val unapprovedWithdraw = Withdraw(0, userUuid, testContext.amount, false, null,
                    null, null, ZonedDateTime.now())
            testContext.withdraws =
                    listOf(withdrawRepository.save(approvedWithdraw), withdrawRepository.save(unapprovedWithdraw))
        }
        suppose("User has a wallet") {
            databaseCleanerService.deleteAllWalletsAndOwners()
            createWalletForUser(userUuid, testContext.walletHash)
        }
        suppose("User service will return user data") {
            Mockito.`when`(userService.getUsers(listOf(userUuid))).thenReturn(listOf(createUserResponse(userUuid)))
        }

        verify("Admin can get list of unapproved withdraws") {
            val result = mockMvc.perform(get("$withdrawPath/approved"))
                    .andExpect(status().isOk)
                    .andReturn()

            val withdrawList: WithdrawWithUserAndAcceptanceListResponse =
                    objectMapper.readValue(result.response.contentAsString)
            assertThat(withdrawList.withdraws).hasSize(1)
            val withdraw = withdrawList.withdraws[0]
            assertThat(withdraw.amount).isEqualTo(testContext.amount)
            assertThat(withdraw.approved).isTrue()
            assertThat(withdraw.approvedReference).isEqualTo(testContext.reference)
            assertThat(withdraw.approvedBy?.uuid).isEqualTo(userUuid)
            assertThat(withdraw.approvedAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(withdraw.user?.uuid).isEqualTo(userUuid)
            assertThat(withdraw.userWallet).isEqualTo(testContext.walletHash)
            assertThat(withdraw.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(withdraw.id).isNotNull()
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PWA_WITHDRAW])
    fun mustBeAbleToApproveWithdraw() {
        suppose("There is unapproved withdraw") {
            val unapprovedWithdraw = Withdraw(0, userUuid, testContext.amount, false, null,
                    null, null, ZonedDateTime.now())
            testContext.withdraws = listOf(withdrawRepository.save(unapprovedWithdraw))
        }

        verify("Admin can approve withdraw") {
            val request = WithdrawApproveRequest(testContext.reference)
            val result = mockMvc.perform(
                    post("$withdrawPath/${testContext.withdraws.first().id}/approve")
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andExpect(status().isOk)
                    .andReturn()

            val withdrawResponse: WithdrawResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(withdrawResponse.user).isEqualTo(userUuid)
            assertThat(withdrawResponse.amount).isEqualTo(testContext.amount)
            assertThat(withdrawResponse.approved).isTrue()
            assertThat(withdrawResponse.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(withdrawResponse.approvedAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(withdrawResponse.approvedBy).isEqualTo(userUuid)
            assertThat(withdrawResponse.approvedReference).isEqualTo(testContext.reference)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PWA_WITHDRAW])
    fun mustNotBeAbleToApproveNonExistingWithdraw() {
        verify("User will get error") {
            val request = WithdrawApproveRequest(testContext.reference)
            val result = mockMvc.perform(
                    post("$withdrawPath/0/approve")
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andExpect(status().isBadRequest)
                    .andReturn()
            verifyResponseErrorCode(result, ErrorCode.WALLET_WITHDRAW_MISSING)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustNotBeAbleToGetWithdrawListWithUserRole() {
        verify("User will get forbidden") {
            mockMvc.perform(get("$withdrawPath/unapproved"))
                    .andExpect(status().isForbidden)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustNotBeAbleToApproveWithdrawWithUserRole() {
        verify("User will get forbidden") {
            val request = WithdrawApproveRequest(testContext.reference)
            mockMvc.perform(
                    post("$withdrawPath/1/approve")
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andExpect(status().isForbidden)
        }
    }

    private class TestContext {
        val amount = 1000L
        val walletHash = "0xa2addee8b62501fb423c8e69a6867a02eaa021a16f66583050a5dd643ad7e41b"
        val reference = "some-bank-reference"
        var withdraws = listOf<Withdraw>()
    }
}
