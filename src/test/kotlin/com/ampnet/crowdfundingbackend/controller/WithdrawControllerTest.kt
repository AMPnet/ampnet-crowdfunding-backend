package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.blockchain.pojo.TransactionData
import com.ampnet.crowdfundingbackend.controller.pojo.request.WithdrawCreateRequest
import com.ampnet.crowdfundingbackend.controller.pojo.response.TransactionResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.WithdrawResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.WithdrawWithUserListResponse
import com.ampnet.crowdfundingbackend.enums.PrivilegeType
import com.ampnet.crowdfundingbackend.enums.TransactionType
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.persistence.model.Withdraw
import com.ampnet.crowdfundingbackend.security.WithMockCrowdfoundUser
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.ZonedDateTime
import java.util.UUID

class WithdrawControllerTest : ControllerTestBase() {

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
        suppose("User has a wallet") {
            databaseCleanerService.deleteAllWalletsAndOwners()
            createWalletForUser(userUuid, testContext.walletHash)
        }

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
            assertThat(withdrawResponse.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(withdrawResponse.approvedAt).isNull()
            assertThat(withdrawResponse.approvedAt).isNull()
            assertThat(withdrawResponse.approvedTxHash).isNull()
            assertThat(withdrawResponse.burnedAt).isNull()
            assertThat(withdrawResponse.burnedTxHash).isNull()
            assertThat(withdrawResponse.burnedBy).isNull()
        }
        verify("Withdraw is created") {
            val withdraws = withdrawRepository.findAll()
            assertThat(withdraws).hasSize(1)
            val withdraw = withdraws.first()
            assertThat(withdraw.userUuid).isEqualTo(userUuid)
            assertThat(withdraw.amount).isEqualTo(testContext.amount)
            assertThat(withdraw.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(withdraw.approvedAt).isNull()
            assertThat(withdraw.approvedTxHash).isNull()
            assertThat(withdraw.burnedAt).isNull()
            assertThat(withdraw.burnedTxHash).isNull()
            assertThat(withdraw.burnedBy).isNull()
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustNotBeAbleToCreateWithdrawWithoutUserWallet() {
        suppose("User does not have a wallet") {
            databaseCleanerService.deleteAllWalletsAndOwners()
        }

        verify("Controller will return bad request") {
            val request = WithdrawCreateRequest(testContext.amount)
            val result = mockMvc.perform(
                    post(withdrawPath)
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andExpect(status().isBadRequest)
                    .andReturn()
            verifyResponseErrorCode(result, ErrorCode.WALLET_MISSING)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_WITHDRAW])
    fun mustBeAbleToGetApprovedWithdraws() {
        suppose("Some withdraws are created") {
            val approvedWithdraw = createApprovedWithdraw(userUuid)
            val unapprovedWithdraw = createWithdraw(userUuid)
            testContext.withdraws = listOf(approvedWithdraw, unapprovedWithdraw)
        }
        suppose("User has a wallet") {
            databaseCleanerService.deleteAllWalletsAndOwners()
            createWalletForUser(userUuid, testContext.walletHash)
        }
        suppose("User service will return user data") {
            Mockito.`when`(userService.getUsers(listOf(userUuid))).thenReturn(listOf(createUserResponse(userUuid)))
        }

        verify("Admin can get list of approved withdraws") {
            val result = mockMvc.perform(get("$withdrawPath/approved"))
                    .andExpect(status().isOk)
                    .andReturn()

            val withdrawList: WithdrawWithUserListResponse =
                    objectMapper.readValue(result.response.contentAsString)
            assertThat(withdrawList.withdraws).hasSize(1)
            val withdraw = withdrawList.withdraws.first()
            assertThat(withdraw.amount).isEqualTo(testContext.amount)
            assertThat(withdraw.id).isNotNull()
            assertThat(withdraw.approvedTxHash).isNotNull()
            assertThat(withdraw.approvedAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(withdraw.user?.uuid).isEqualTo(userUuid)
            assertThat(withdraw.userWallet).isEqualTo(testContext.walletHash)
            assertThat(withdraw.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(withdraw.burnedAt).isNull()
            assertThat(withdraw.burnedBy).isNull()
            assertThat(withdraw.burnedTxHash).isNull()
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_WITHDRAW])
    fun mustBeAbleToGetBurnedWithdraws() {
        suppose("Some withdraws are created") {
            val approvedWithdraw = createApprovedWithdraw(userUuid)
            val burnedWithdraw = createBurnedWithdraw(userUuid)
            testContext.withdraws = listOf(approvedWithdraw, burnedWithdraw)
        }
        suppose("User has a wallet") {
            databaseCleanerService.deleteAllWalletsAndOwners()
            createWalletForUser(userUuid, testContext.walletHash)
        }
        suppose("User service will return user data") {
            Mockito.`when`(userService.getUsers(listOf(userUuid))).thenReturn(listOf(createUserResponse(userUuid)))
        }

        verify("Admin can get list of burned withdraws") {
            val result = mockMvc.perform(get("$withdrawPath/burned"))
                    .andExpect(status().isOk)
                    .andReturn()

            val withdrawList: WithdrawWithUserListResponse =
                    objectMapper.readValue(result.response.contentAsString)
            assertThat(withdrawList.withdraws).hasSize(1)
            val withdraw = withdrawList.withdraws.first()
            assertThat(withdraw.amount).isEqualTo(testContext.amount)
            assertThat(withdraw.id).isNotNull()
            assertThat(withdraw.approvedTxHash).isNotNull()
            assertThat(withdraw.approvedAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(withdraw.user?.uuid).isEqualTo(userUuid)
            assertThat(withdraw.userWallet).isEqualTo(testContext.walletHash)
            assertThat(withdraw.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(withdraw.burnedAt).isNotNull()
            assertThat(withdraw.burnedBy).isNotNull()
            assertThat(withdraw.burnedTxHash).isNotNull()
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustNotBeAbleToGetWithdrawListWithUserRole() {
        verify("User will get forbidden") {
            mockMvc.perform(get("$withdrawPath/approved"))
                    .andExpect(status().isForbidden)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGenerateApprovalTransaction() {
        suppose("Transaction info is clean") {
            databaseCleanerService.deleteAllTransactionInfo()
        }
        suppose("User has a wallet") {
            databaseCleanerService.deleteAllWalletsAndOwners()
            createWalletForUser(userUuid, testContext.walletHash)
        }
        suppose("User has created withdraw") {
            testContext.withdraw = createWithdraw(userUuid)
        }
        suppose("Blockchain service will return approve burn transaction") {
            testContext.transactionData = generateTransactionData("approve-burn-transaction")
            Mockito.`when`(
                    blockchainService.generateApproveBurnTransaction(testContext.walletHash, testContext.amount)
            ).thenReturn(testContext.transactionData)
        }

        verify("User can generate approval transaction") {
            val result = mockMvc.perform(post("$withdrawPath/${testContext.withdraw.id}/transaction/approve"))
                    .andExpect(status().isOk)
                    .andReturn()

            val transactionResponse: TransactionResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(transactionResponse.tx).isEqualTo(testContext.transactionData)
            assertThat(transactionResponse.txId).isNotNull()
            assertThat(transactionResponse.info.txType).isEqualTo(TransactionType.BURN_APPROVAL)
        }
        verify("Transaction info is created") {
            val transactionInfos = transactionInfoRepository.findAll()
            assertThat(transactionInfos).hasSize(1)
            val transactionInfo = transactionInfos.first()
            assertThat(transactionInfo.companionId).isEqualTo(testContext.withdraw.id)
            assertThat(transactionInfo.type).isEqualTo(TransactionType.BURN_APPROVAL)
            assertThat(transactionInfo.userUuid).isEqualTo(userUuid)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PWA_WITHDRAW])
    fun mustBeAbleToGenerateBurnTransaction() {
        suppose("Transaction info is clean") {
            databaseCleanerService.deleteAllTransactionInfo()
        }
        suppose("User has a wallet") {
            databaseCleanerService.deleteAllWalletsAndOwners()
            createWalletForUser(userUuid, testContext.walletHash)
        }
        suppose("User has created withdraw") {
            testContext.withdraw = createApprovedWithdraw(userUuid)
        }
        suppose("Blockchain service will return approve burn transaction") {
            testContext.transactionData = generateTransactionData("approve-burn-transaction")
            Mockito.`when`(blockchainService.generateBurnTransaction(
                    "0x43b0d9b605e68a0c50dc436757a86c82d97787cc", testContext.walletHash, testContext.amount)
            ).thenReturn(testContext.transactionData)
        }

        verify("User can generate approval transaction") {
            val result = mockMvc.perform(post("$withdrawPath/${testContext.withdraw.id}/transaction/burn"))
                    .andExpect(status().isOk)
                    .andReturn()

            val transactionResponse: TransactionResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(transactionResponse.tx).isEqualTo(testContext.transactionData)
            assertThat(transactionResponse.txId).isNotNull()
            assertThat(transactionResponse.info.txType).isEqualTo(TransactionType.BURN)
        }
        verify("Transaction info is created") {
            val transactionInfos = transactionInfoRepository.findAll()
            assertThat(transactionInfos).hasSize(1)
            val transactionInfo = transactionInfos.first()
            assertThat(transactionInfo.companionId).isEqualTo(testContext.withdraw.id)
            assertThat(transactionInfo.type).isEqualTo(TransactionType.BURN)
            assertThat(transactionInfo.userUuid).isEqualTo(userUuid)
        }
    }

    private fun createBurnedWithdraw(user: UUID): Withdraw {
        val withdraw = Withdraw(0, user, testContext.amount, ZonedDateTime.now(),
                "approved-tx", ZonedDateTime.now(),
                "burned-tx", ZonedDateTime.now(), UUID.randomUUID())
        return withdrawRepository.save(withdraw)
    }

    private class TestContext {
        val amount = 1000L
        val walletHash = "0xa2addee8b62501fb423c8e69a6867a02eaa021a16f66583050a5dd643ad7e41b"
        var withdraws = listOf<Withdraw>()
        lateinit var withdraw: Withdraw
        lateinit var transactionData: TransactionData
    }
}
