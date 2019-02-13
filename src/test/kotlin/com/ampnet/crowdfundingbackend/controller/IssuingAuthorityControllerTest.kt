package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.request.SignedTransactionRequest
import com.ampnet.crowdfundingbackend.controller.pojo.response.TransactionResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.TxHashResponse
import com.ampnet.crowdfundingbackend.service.pojo.PostTransactionType
import com.ampnet.crowdfundingbackend.service.pojo.TransactionData
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

    @Test
    fun mustBeAbleToGenerateAndPostMintTransaction() {
        suppose("Blockchain service will generate mint transaction") {
            Mockito.`when`(
                blockchainService.generateMintTransaction(testContext.from, testContext.toHash, testContext.amount)
            ).thenReturn(testContext.transactionData)
        }
        verify("User can get mint transaction") {
            val result = mockMvc.perform(
                MockMvcRequestBuilders.get("$pathIssuer/mint")
                    .param("amount", testContext.amount.toString())
                    .param("toHash", testContext.toHash)
                    .param("from", testContext.from))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            testContext.transactionResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(testContext.transactionResponse.transactionData).isEqualTo(testContext.transactionData)
            assertThat(testContext.transactionResponse.link).isEqualTo("/issuer/transaction?type=mint")
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
    fun mustBeAbleToGenerateAndPostBurnTransaction() {
        suppose("Blockchain service will generate burn transaction") {
            Mockito.`when`(
                blockchainService.generateBurnTransaction(
                    testContext.from,
                    testContext.burnFromTxHash,
                    testContext.amount)
            ).thenReturn(testContext.transactionData)
        }
        verify("User can get burn transaction") {
            val result = mockMvc.perform(
                MockMvcRequestBuilders.get("$pathIssuer/burn")
                    .param("amount", testContext.amount.toString())
                    .param("burnFromTxHash", testContext.burnFromTxHash)
                    .param("from", testContext.from))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            testContext.transactionResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(testContext.transactionResponse.transactionData).isEqualTo(testContext.transactionData)
            assertThat(testContext.transactionResponse.link).isEqualTo("/issuer/transaction?type=burn")
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
    fun mustNotBeAbleToPostOtherTransactions() {
        val request = SignedTransactionRequest(testContext.signedTransaction)
        mockMvc.perform(
            MockMvcRequestBuilders.post("$pathIssuer/transaction?type=invalid")
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
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
        val toHash = "0xcb09b18b3d46fc012769e000c8ac2fc67547b79d24043cf00f675529fc8607fb"
        val amount = 100L
        val burnFromTxHash = "0x28cc5dc617d2f559e4735995c99e19cf80aa502c0e55bf938328ed1fa2976d20"
        val transactionData = TransactionData("data", "to", 22, 33, 44, amount, "pubg")
        val signedTransaction = "SignedTransaction"
        val txHash = "0xa2addee8b62501fb423c8e69a6867a02eaa021a16f66583050a5dd643ad7e41b"
        lateinit var transactionResponse: TransactionResponse
    }
}
