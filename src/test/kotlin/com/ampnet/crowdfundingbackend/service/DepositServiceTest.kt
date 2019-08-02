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
                transactionInfoService, storageServiceImpl)
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
            createApprovedDeposit()
        }

        verify("Service will throw exception for existing unapproved deposit") {
            assertThrows<ResourceAlreadyExistsException> {
                depositService.create(userUuid)
            }
        }
    }

    @Test
    fun mustThrowExceptionIfDepositIsMissingForMintTransaction() {
        verify("Service will throw exception if the deposit is missing") {
            assertThrows<ResourceNotFoundException> {
                val request = MintServiceRequest("to_wallet", 1000, userUuid, 0)
                depositService.generateMintTransaction(request)
            }
        }
    }

    @Test
    fun mustThrowExceptionIfDepositIsMintedForMintTransaction() {
        suppose("Deposit is already minted") {
            testContext.deposit = createApprovedDeposit()
        }

        verify("Service will throw exception if the deposit already has tx hash") {
            assertThrows<ResourceAlreadyExistsException> {
                val request = MintServiceRequest("to_wallet", 1000, userUuid, testContext.deposit.id)
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
                val request = MintServiceRequest("to_wallet", 1000, userUuid, testContext.deposit.id)
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
            testContext.deposit = createApprovedDeposit()
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

    private fun createApprovedDeposit(): Deposit {
        val document = saveDocument("doc", "doc-lni", userUuid, "type", 1)
        val deposit = Deposit(0, userUuid, "S34SDGFT", true,
                userUuid, ZonedDateTime.now(), 10_000, document, "tx_hash", ZonedDateTime.now()
        )
        return depositRepository.save(deposit)
    }

    private fun createUnapprovedDeposit(): Deposit {
        val deposit = Deposit(0, userUuid, "S34SDGFT", false,
                null, null, null, null, null, ZonedDateTime.now()
        )
        return depositRepository.save(deposit)
    }

    private class TestContext {
        lateinit var deposit: Deposit
    }
}
