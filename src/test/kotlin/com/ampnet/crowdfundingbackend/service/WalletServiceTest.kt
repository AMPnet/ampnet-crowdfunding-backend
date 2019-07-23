package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.controller.pojo.request.WalletCreateRequest
import com.ampnet.crowdfundingbackend.exception.ResourceAlreadyExistsException
import com.ampnet.crowdfundingbackend.enums.Currency
import com.ampnet.crowdfundingbackend.enums.WalletType
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.Wallet
import com.ampnet.crowdfundingbackend.service.impl.TransactionInfoServiceImpl
import com.ampnet.crowdfundingbackend.service.impl.WalletServiceImpl
import com.ampnet.crowdfundingbackend.service.pojo.PostTransactionType
import com.ampnet.crowdfundingbackend.blockchain.pojo.TransactionData
import com.ampnet.crowdfundingbackend.persistence.model.PairWalletCode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.time.ZonedDateTime

class WalletServiceTest : JpaServiceTestBase() {

    private val walletService: WalletService by lazy {
        val transactionService = TransactionInfoServiceImpl(transactionInfoRepository)
        WalletServiceImpl(walletRepository, projectRepository, organizationRepository, userWalletRepository,
                mockedBlockchainService, transactionService, pairWalletCodeRepository)
    }
    private lateinit var testContext: TestContext

    private val defaultAddress = "0x14bC6a8219c798394726f8e86E040A878da1d99D"
    private val defaultAddressHash = "0x4e4ee58ff3a9e9e78c2dfdbac0d1518e4e1039f9189267e1dc8d3e35cbdf7892"
    private val defaultPublicKey = "0xC2D7CF95645D33006175B78989035C7c9061d3F9"
    private val defaultSignedTransaction = "SignedTransaction"
    private val defaultTransactionData = TransactionData("data", "to", 1, 1, 1, 1, "public_key")

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllWalletsAndOwners()
        testContext = TestContext()
    }

    @Test
    fun mustBeAbleToGetWalletForUserId() {
        suppose("User has a wallet") {
            createWalletForUser(userUuid, defaultAddress)
        }

        verify("Service must fetch wallet for user with id") {
            val wallet = walletService.getUserWallet(userUuid) ?: fail("User must have a wallet")
            assertThat(wallet.hash).isEqualTo(defaultAddress)
            assertThat(wallet.currency).isEqualTo(Currency.EUR)
            assertThat(wallet.type).isEqualTo(WalletType.USER)
        }
    }

    @Test
    fun mustBeAbleToCreateWalletForUser() {
        suppose("Blockchain service successfully adds wallet") {
            Mockito.`when`(mockedBlockchainService.addWallet(defaultAddress, defaultPublicKey))
                    .thenReturn(defaultAddressHash)
        }
        suppose("Wallet has pair wallet code") {
            databaseCleanerService.deleteAllPairWalletCodes()
            val pairWalletCode = PairWalletCode(0, defaultAddress, defaultPublicKey, "000000", ZonedDateTime.now())
            pairWalletCodeRepository.save(pairWalletCode)
        }

        verify("Service can create wallet for a user") {
            val request = WalletCreateRequest(defaultAddress, defaultPublicKey)
            val wallet = walletService.createUserWallet(userUuid, request)
            assertThat(wallet.hash).isEqualTo(defaultAddressHash)
            assertThat(wallet.currency).isEqualTo(Currency.EUR)
            assertThat(wallet.type).isEqualTo(WalletType.USER)
            assertThat(wallet.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
        }
        verify("Wallet is assigned to the user") {
            val wallet = walletService.getUserWallet(userUuid) ?: fail("User must have a wallet")
            assertThat(wallet.hash).isEqualTo(defaultAddressHash)
        }
        verify("Pair wallet code is deleted") {
            val optionalPairWalletCode = pairWalletCodeRepository.findByAddress(defaultAddress)
            assertThat(optionalPairWalletCode).isNotPresent
        }
    }

    @Test
    fun mustBeAbleToCreateWalletForProject() {
        suppose("Project exists") {
            val organization = createOrganization("Org", userUuid)
            testContext.project = createProject("Das project", organization, userUuid)
        }
        suppose("Blockchain service successfully adds wallet") {
            Mockito.`when`(
                mockedBlockchainService.postTransaction(defaultSignedTransaction, PostTransactionType.PRJ_CREATE)
            ).thenReturn(defaultAddressHash)
        }

        verify("Service can create wallet for project") {
            val wallet = walletService.createProjectWallet(testContext.project, defaultSignedTransaction)
            assertThat(wallet.hash).isEqualTo(defaultAddressHash)
            assertThat(wallet.currency).isEqualTo(Currency.EUR)
            assertThat(wallet.type).isEqualTo(WalletType.PROJECT)
            assertThat(wallet.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
        }
        verify("Wallet is assigned to the project") {
            val optionalProjectWithWallet = projectRepository.findByIdWithWallet(testContext.project.id)
            assertThat(optionalProjectWithWallet).isPresent
            val walletHash = getWalletHash(optionalProjectWithWallet.get().wallet)
            assertThat(walletHash).isEqualTo(defaultAddressHash)
        }
    }

    @Test
    fun mustNotBeAbleToCreateMultipleWalletsForOneUser() {
        suppose("User has a wallet") {
            createWalletForUser(userUuid, defaultAddressHash)
        }

        verify("Service cannot create additional account") {
            val exception = assertThrows<ResourceAlreadyExistsException> {
                val request = WalletCreateRequest(defaultAddress, defaultPublicKey)
                walletService.createUserWallet(userUuid, request)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_EXISTS)
        }
    }

    @Test
    fun mustNotBeAbleToCreateMultipleWalletsForOneProject() {
        suppose("Project exists") {
            val organization = createOrganization("Org", userUuid)
            testContext.project = createProject("Das project", organization, userUuid)
        }
        suppose("Project has a wallet") {
            createWalletForProject(testContext.project, defaultAddressHash)
        }

        verify("Service cannot create additional account") {
            val exception = assertThrows<ResourceAlreadyExistsException> {
                walletService.createProjectWallet(testContext.project, defaultSignedTransaction)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_EXISTS)
        }
    }

    @Test
    fun mustBeAbleToGetWalletBalance() {
        suppose("User has a wallet") {
            createWalletForUser(userUuid, defaultAddressHash)
        }
        suppose("User has some funds on a wallet") {
            testContext.balance = 100
            Mockito.`when`(mockedBlockchainService.getBalance(defaultAddressHash)).thenReturn(testContext.balance)
        }

        verify("Service can return wallet balance") {
            val wallet = walletService.getUserWallet(userUuid) ?: fail("User must have a wallet")
            val balance = walletService.getWalletBalance(wallet)
            assertThat(balance).isEqualTo(testContext.balance)
        }
    }

    @Test
    fun mustThrowExceptionIfUserWithoutWalletTriesToGenerateCreateProjectWallet() {
        suppose("Project exists") {
            val organization = createOrganization("Org", userUuid)
            testContext.project = createProject("Das project", organization, userUuid)
        }

        verify("Service will throw InternalException") {
            val exception = assertThrows<ResourceNotFoundException> {
                walletService.generateTransactionToCreateProjectWallet(testContext.project, userUuid)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_MISSING)
        }
    }

    @Test
    fun mustThrowExceptionIfOrganizationWithoutWalletTriesToGenerateCreateProjectWallet() {
        suppose("User has a wallet") {
            createWalletForUser(userUuid, defaultAddressHash)
        }
        suppose("Project exists") {
            val organization = createOrganization("Org", userUuid)
            testContext.project = createProject("Das project", organization, userUuid)
        }

        verify("Service will throw InternalException") {
            val exception = assertThrows<ResourceNotFoundException> {
                walletService.generateTransactionToCreateProjectWallet(testContext.project, userUuid)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_MISSING)
        }
    }

    @Test
    fun mustThrowExceptionWhenGenerateTransactionToCreateOrganizationWalletWithoutUserWallet() {
        suppose("Organization exists") {
            testContext.organization = createOrganization("Org", userUuid)
        }

        verify("Service can generate create organization transaction") {
            val exception = assertThrows<ResourceNotFoundException> {
                walletService.generateTransactionToCreateOrganizationWallet(testContext.organization, userUuid)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_MISSING)
        }
    }

    @Test
    fun mustThrowExceptionIfOrganizationAlreadyHasWallet() {
        suppose("Organization exists") {
            testContext.organization = createOrganization("Org", userUuid)
        }
        suppose("Organization has a wallet") {
            createWalletForOrganization(testContext.organization, defaultAddressHash)
        }

        verify("Service will throw exception that organization already has a wallet") {
            val exception = assertThrows<ResourceAlreadyExistsException> {
                walletService.generateTransactionToCreateOrganizationWallet(testContext.organization, userUuid)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_EXISTS)
        }
    }

    @Test
    fun mustGenerateTransactionToCreateOrganizationWallet() {
        suppose("User has a wallet") {
            testContext.wallet = createWalletForUser(userUuid, defaultAddress)
        }
        suppose("Organization exists") {
            testContext.organization = createOrganization("Org", userUuid)
        }
        suppose("Blockchain service will generate transaction") {
            Mockito.`when`(
                    mockedBlockchainService.generateAddOrganizationTransaction(
                            testContext.wallet.hash, testContext.organization.name)
            ).thenReturn(defaultTransactionData)
        }

        verify("Service can generate transaction") {
            val transaction = walletService.generateTransactionToCreateOrganizationWallet(
                    testContext.organization, userUuid)
            assertThat(transaction.transactionData).isEqualTo(defaultTransactionData)
        }
    }

    @Test
    fun mustBeAbleToCreateOrganizationWallet() {
        suppose("Organization exists") {
            testContext.organization = createOrganization("Org", userUuid)
        }
        suppose("Blockchain service successfully adds wallet") {
            Mockito.`when`(
                mockedBlockchainService.postTransaction(defaultSignedTransaction, PostTransactionType.ORG_CREATE)
            ).thenReturn(defaultAddressHash)
        }

        verify("Service can create wallet for organization") {
            val wallet = walletService.createOrganizationWallet(testContext.organization, defaultSignedTransaction)
            assertThat(wallet.hash).isEqualTo(defaultAddressHash)
            assertThat(wallet.currency).isEqualTo(Currency.EUR)
            assertThat(wallet.type).isEqualTo(WalletType.ORG)
            assertThat(wallet.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
        }
        verify("Wallet is assigned to the organization") {
            val optionalOrganization = organizationRepository.findById(testContext.organization.id)
            assertThat(optionalOrganization).isPresent
            val walletHash = getWalletHash(optionalOrganization.get().wallet)
            assertThat(walletHash).isEqualTo(defaultAddressHash)
        }
    }

    @Test
    fun mustThrowExceptionForCreateOrganizationWalletIfOrganizationAlreadyHasWallet() {
        suppose("Organization exists") {
            testContext.organization = createOrganization("Org", userUuid)
        }
        suppose("Organization has a wallet") {
            createWalletForOrganization(testContext.organization, defaultAddressHash)
        }

        verify("Service cannot create additional organization account") {
            val exception = assertThrows<ResourceAlreadyExistsException> {
                walletService.createOrganizationWallet(testContext.organization, defaultSignedTransaction)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_EXISTS)
        }
    }

    @Test
    fun mustNotBeAbleToCreateWalletWithTheSameHash() {
        suppose("User has a wallet") {
            createWalletForUser(userUuid, defaultAddressHash)
        }
        suppose("Project exists") {
            val organization = createOrganization("Org", userUuid)
            testContext.project = createProject("Das project", organization, userUuid)
        }
        suppose("Blockchain service will return same hash for new project wallet transaction") {
            Mockito.`when`(
                mockedBlockchainService.postTransaction(defaultSignedTransaction, PostTransactionType.PRJ_CREATE)
            ).thenReturn(defaultAddressHash)
        }

        verify("User will not be able to create organization wallet with the same hash") {
            val exception = assertThrows<ResourceAlreadyExistsException> {
                walletService.createProjectWallet(testContext.project, defaultSignedTransaction)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_HASH_EXISTS)
        }
    }

    @Test
    fun mustGenerateNewPairWalletCodeForExistingAddress() {
        suppose("Pair wallet code exists") {
            databaseCleanerService.deleteAllPairWalletCodes()
            val pairWalletCode = PairWalletCode(0, "0x00000", "adr_423242", "SD432X", ZonedDateTime.now())
            testContext.pairWalletCode = pairWalletCodeRepository.save(pairWalletCode)
        }

        verify("Service will create new pair wallet code") {
            val requests = WalletCreateRequest(testContext.pairWalletCode.address, testContext.pairWalletCode.publicKey)
            val newPairWalletCode = walletService.generatePairWalletCode(requests)
            assertThat(newPairWalletCode.address).isEqualTo(testContext.pairWalletCode.address)
            assertThat(newPairWalletCode.publicKey).isEqualTo(testContext.pairWalletCode.publicKey)
        }
        verify("Old pair wallet code is deleted") {
            val oldPairWalletCode = pairWalletCodeRepository.findById(testContext.pairWalletCode.id)
            assertThat(oldPairWalletCode).isNotPresent
        }
    }

    private class TestContext {
        lateinit var organization: Organization
        lateinit var project: Project
        lateinit var wallet: Wallet
        var balance: Long = -1
        lateinit var pairWalletCode: PairWalletCode
    }
}
