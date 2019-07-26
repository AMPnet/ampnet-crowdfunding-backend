package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.request.SignedTransactionRequest
import com.ampnet.crowdfundingbackend.controller.pojo.response.TransactionAndLinkResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.TxHashResponse
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.service.pojo.PostTransactionType
import com.ampnet.crowdfundingbackend.blockchain.pojo.TransactionData
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

class IssuingAuthorityControllerTest : ControllerTestBase() {

    private val pathIssuer = "/issuer"
    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        testContext = TestContext()
    }

    /* Mint Transaction */
    @Test
    fun mustBeAbleToGenerateMintTransaction() {
        suppose("User has a wallet") {
            databaseCleanerService.deleteAllWalletsAndOwners()
            createWalletForUser(userUuid, testContext.userWalletHash)
        }
        suppose("Blockchain service will generate mint transaction") {
            val userWalletHash = getUserWalletHash(userUuid)
            Mockito.`when`(
                blockchainService.generateMintTransaction(testContext.from, userWalletHash, testContext.amount)
            ).thenReturn(testContext.transactionData)
        }

        verify("User can get mint transaction") {
            val result = mockMvc.perform(
                MockMvcRequestBuilders.get("$pathIssuer/mint")
                    .param("amount", testContext.amount.toString())
                    .param("uuid", userUuid.toString())
                    .param("from", testContext.from))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            testContext.transactionResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(testContext.transactionResponse.tx).isEqualTo(testContext.transactionData)
            assertThat(testContext.transactionResponse.link).isEqualTo("/issuer/transaction/mint")
        }
    }

    @Test
    fun mustBeAbleToPostMintTransaction() {
        suppose("Issuing authority has mint transaction") {
            testContext.transactionResponse =
                    TransactionAndLinkResponse(testContext.transactionData, "/issuer/transaction/mint")
        }
        suppose("Blockchain service will accept signed transaction") {
            Mockito.`when`(
                blockchainService.postTransaction(testContext.signedTransaction, PostTransactionType.ISSUER_MINT)
            ).thenReturn(testContext.txHash)
        }

        verify("User can post signed mint transaction") {
            verifyPostTransaction()
        }
    }

    @Test
    fun mustNotBeAbleToGenerateMintTransactionForMissingUserWallet() {
        suppose("User does not have a wallet") {
            databaseCleanerService.deleteAllWalletsAndOwners()
        }

        verify("User cannot generate burn transaction if the user wallet is missing") {
            val response = mockMvc.perform(
                    MockMvcRequestBuilders.get("$pathIssuer/mint")
                            .param("amount", testContext.amount.toString())
                            .param("uuid", userUuid.toString())
                            .param("from", testContext.from))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest)
                    .andReturn()

            verifyResponseErrorCode(response, ErrorCode.WALLET_MISSING)
        }
    }

    @Test
    fun mustThrowBadRequestForInvalidUserUuid() {

        verify("User cannot generate burn transaction if the user wallet is missing") {
            mockMvc.perform(
                    MockMvcRequestBuilders.get("$pathIssuer/mint")
                            .param("amount", testContext.amount.toString())
                            .param("uuid", "invalid-uuid")
                            .param("from", testContext.from))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest)
        }
    }

    /* Burn Transaction */
    @Test
    fun mustBeAbleToGenerateBurnTransaction() {
        suppose("User has a wallet") {
            databaseCleanerService.deleteAllWalletsAndOwners()
            createWalletForUser(userUuid, testContext.userWalletHash)
        }
        suppose("Blockchain service will generate burn transaction") {
            val userWalletHash = getUserWalletHash(userUuid)
            Mockito.`when`(
                blockchainService.generateBurnTransaction(testContext.from, userWalletHash, testContext.amount)
            ).thenReturn(testContext.transactionData)
        }

        verify("User can get burn transaction") {
            val result = mockMvc.perform(
                MockMvcRequestBuilders.get("$pathIssuer/burn")
                    .param("amount", testContext.amount.toString())
                    .param("uuid", userUuid.toString())
                    .param("from", testContext.from))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            testContext.transactionResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(testContext.transactionResponse.tx).isEqualTo(testContext.transactionData)
            assertThat(testContext.transactionResponse.link).isEqualTo("/issuer/transaction/burn")
        }
    }

    @Test
    fun mustBeAbleToPostBurnTransaction() {
        suppose("Issuing authority has burn transaction") {
            testContext.transactionResponse =
                    TransactionAndLinkResponse(testContext.transactionData, "/issuer/transaction/burn")
        }
        suppose("Blockchain service will accept signed transaction") {
            Mockito.`when`(
                blockchainService.postTransaction(testContext.signedTransaction, PostTransactionType.ISSUER_BURN)
            ).thenReturn(testContext.txHash)
        }

        verify("User can post signed burn transaction") {
            verifyPostTransaction()
        }
    }

    @Test
    fun mustNotBeAbleToGenerateBurnTransactionForMissingUserWallet() {
        suppose("User does not have a wallet") {
            databaseCleanerService.deleteAllWalletsAndOwners()
        }

        verify("User cannot generate burn transaction if the user wallet is missing") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("$pathIssuer/burn")
                    .param("amount", testContext.amount.toString())
                    .param("uuid", userUuid.toString())
                    .param("from", testContext.from))
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.WALLET_MISSING)
        }
    }

    /* Invalid Transaction*/
    @Test
    fun mustNotBeAbleToPostOtherTransactions() {
        verify("Issuing authority is not able to post invalid transaction type") {
            val request = SignedTransactionRequest(testContext.signedTransaction)
            mockMvc.perform(
                MockMvcRequestBuilders.post("$pathIssuer/transaction/invalid")
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
        }
    }

    private fun verifyPostTransaction() {
        val request = SignedTransactionRequest(testContext.signedTransaction)
        val result = mockMvc.perform(
            MockMvcRequestBuilders.post(testContext.transactionResponse.link)
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val txHashResponse: TxHashResponse = objectMapper.readValue(result.response.contentAsString)
        assertThat(txHashResponse.txHash).isEqualTo(testContext.txHash)
    }

    private class TestContext {
        val from = "0x43b0d9b605e68a0c50dc436757a86c82d97787cc"
        val userWalletHash = "0xa2addee8b62501fb423c8e69a6867a02eaa021a16f66583050a5dd643ad7e41b"
        val amount = 100L
        val transactionData = TransactionData("data", "to", 22, 33, 44, amount, "pubg")
        val signedTransaction = "SignedTransaction"
        val txHash = "0xa2addee8b62501fb423c8e69a6867a02eaa021a16f66583050a5dd643ad7e41b"
        lateinit var transactionResponse: TransactionAndLinkResponse
    }
}
