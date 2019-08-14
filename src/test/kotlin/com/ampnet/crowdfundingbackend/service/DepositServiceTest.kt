package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.exception.InvalidRequestException
import com.ampnet.crowdfundingbackend.exception.ResourceAlreadyExistsException
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.persistence.model.Deposit
import com.ampnet.crowdfundingbackend.persistence.repository.DepositRepository
import com.ampnet.crowdfundingbackend.service.impl.DepositServiceImpl
import com.ampnet.crowdfundingbackend.service.impl.StorageServiceImpl
import com.ampnet.crowdfundingbackend.service.impl.TransactionInfoServiceImpl
import com.ampnet.crowdfundingbackend.service.pojo.MintServiceRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.time.ZonedDateTime

class DepositServiceTest : JpaServiceTestBase() {

    @Autowired
    private lateinit var depositRepository: DepositRepository

    private val depositService: DepositService by lazy {
        val storageServiceImpl = StorageServiceImpl(documentRepository, cloudStorageService)
        val transactionInfoService = TransactionInfoServiceImpl(transactionInfoRepository)
        DepositServiceImpl(depositRepository, userWalletRepository, mockedBlockchainService,
                transactionInfoService, storageServiceImpl, mailService)
    }
    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllDeposits()
        testContext = TestContext()
    }

    @Test
    fun mustThrowExceptionIfUnapprovedDepositExistsForCreatingDeposit() {
        suppose("User has a wallet") {
            databaseCleanerService.deleteAllWalletsAndOwners()
            createWalletForUser(userUuid, "wallet-hash")
        }
        suppose("Unapproved and approved deposits exists") {
            createUnapprovedDeposit()
            createApprovedDeposit("tx_hash")
        }

        verify("Service will throw exception for existing unapproved deposit") {
            assertThrows<ResourceAlreadyExistsException> {
                depositService.create(userUuid, 100L)
            }
        }
    }

    @Test
    fun mustThrowExceptionIfReceivingUserDoesNotHaveWallet() {
        suppose("User does not have a wallet") {
            databaseCleanerService.deleteAllWalletsAndOwners()
        }
        suppose("Unapproved deposit exist") {
            testContext.deposit = createApprovedDeposit(null)
        }

        verify("Service will throw exception for existing unapproved deposit") {
            assertThrows<ResourceNotFoundException> {
                val request = MintServiceRequest(testContext.deposit.id, userUuid)
                depositService.generateMintTransaction(request)
            }
        }
    }

    @Test
    fun mustThrowExceptionIfDepositIsMissingForMintTransaction() {
        verify("Service will throw exception if the deposit is missing") {
            assertThrows<ResourceNotFoundException> {
                val request = MintServiceRequest(0, userUuid)
                depositService.generateMintTransaction(request)
            }
        }
    }

    @Test
    fun mustThrowExceptionIfDepositIsMintedForMintTransaction() {
        suppose("Deposit is already minted") {
            testContext.deposit = createApprovedDeposit("tx_hash")
        }

        verify("Service will throw exception if the deposit already has tx hash") {
            assertThrows<ResourceAlreadyExistsException> {
                val request = MintServiceRequest(testContext.deposit.id, userUuid)
                depositService.generateMintTransaction(request)
            }
        }
    }

    @Test
    fun mustThrowExceptionIfDepositIsNotApprovedForMintTransaction() {
        suppose("Deposit is not approved") {
            testContext.deposit = createUnapprovedDeposit()
        }

        verify("Service will throw exception if the deposit is not approved") {
            assertThrows<InvalidRequestException> {
                val request = MintServiceRequest(testContext.deposit.id, userUuid)
                depositService.generateMintTransaction(request)
            }
        }
    }

    @Test
    fun mustThrowExceptionIfDepositIsMissingForConfirmMintTransaction() {
        verify("Service will throw exception if the deposit is missing") {
            assertThrows<ResourceNotFoundException> {
                depositService.confirmMintTransaction("signed-transaction", 0)
            }
        }
    }

    @Test
    fun mustThrowExceptionIfDepositIsMintedForConfirmMintTransaction() {
        suppose("Deposit is already minted") {
            testContext.deposit = createApprovedDeposit("tx_hash")
        }

        verify("Service will throw exception if the deposit already has tx hash") {
            assertThrows<ResourceAlreadyExistsException> {
                depositService.confirmMintTransaction("signed-transaction", testContext.deposit.id)
            }
        }
    }

    @Test
    fun mustThrowExceptionIfDepositIsNotApprovedForConfirmMintTransaction() {
        suppose("Deposit is not approved") {
            testContext.deposit = createUnapprovedDeposit()
        }

        verify("Service will throw exception if the deposit is not approved") {
            assertThrows<InvalidRequestException> {
                depositService.confirmMintTransaction("signed-transaction", testContext.deposit.id)
            }
        }
    }

    @Test
    fun mustThrowExceptionForDeletingMintedDeposit() {
        suppose("Deposit is minted") {
            testContext.deposit = createApprovedDeposit("tx-hash")
        }

        verify("User cannot delete minted deposit") {
            assertThrows<InvalidRequestException> {
                depositService.delete(testContext.deposit.id)
            }
        }
    }

    private fun createApprovedDeposit(txHash: String?): Deposit {
        val document = saveDocument("doc", "doc-lni", userUuid, "type", 1)
        val deposit = Deposit(0, userUuid, "S34SDGFT", true, 10_000,
                userUuid, ZonedDateTime.now(), document, txHash, ZonedDateTime.now())
        return depositRepository.save(deposit)
    }

    private fun createUnapprovedDeposit(): Deposit {
        val deposit = Deposit(0, userUuid, "S34SDGFT", false, 10_000,
                null, null, null, null, ZonedDateTime.now())
        return depositRepository.save(deposit)
    }

    private class TestContext {
        lateinit var deposit: Deposit
    }
}
