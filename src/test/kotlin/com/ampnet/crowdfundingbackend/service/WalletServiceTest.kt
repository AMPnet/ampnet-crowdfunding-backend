package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.controller.pojo.request.WalletCreateRequest
import com.ampnet.crowdfundingbackend.exception.ResourceAlreadyExistsException
import com.ampnet.crowdfundingbackend.enums.Currency
import com.ampnet.crowdfundingbackend.enums.WalletType
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.persistence.model.Wallet
import com.ampnet.crowdfundingbackend.service.impl.UserServiceImpl
import com.ampnet.crowdfundingbackend.service.impl.WalletServiceImpl
import com.ampnet.crowdfundingbackend.service.pojo.PostTransactionType
import com.ampnet.crowdfundingbackend.service.pojo.TransactionData
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.time.ZonedDateTime

class WalletServiceTest : JpaServiceTestBase() {

    private val userService: UserService by lazy {
        val mailService = Mockito.mock(MailService::class.java)
        UserServiceImpl(userRepository, roleRepository, countryRepository, mailTokenRepository,
                mailService, passwordEncoder, applicationProperties)
    }
    private val walletService: WalletService by lazy {
        WalletServiceImpl(walletRepository, userRepository, projectRepository, organizationRepository,
            mockedBlockchainService)
    }
    private lateinit var user: User
    private lateinit var testContext: TestContext

    private val defaultAddress = "0x14bC6a8219c798394726f8e86E040A878da1d99D"
    private val defaultAddressHash = "0x4e4ee58ff3a9e9e78c2dfdbac0d1518e4e1039f9189267e1dc8d3e35cbdf7892"
    private val defaultPublicKey = "0xC2D7CF95645D33006175B78989035C7c9061d3F9"
    private val defaultSignedTransaction = "SignedTransaction"
    private val defaultTransactionData = TransactionData("data", "to", 1, 1, 1, 1, "public_key")

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllWalletsAndOwners()
        user = createUser("test@email.com", "First", "Last")
        testContext = TestContext()
    }

    @Test
    fun mustBeAbleToGetWalletForUserId() {
        suppose("User has a wallet") {
            createWalletForUser(user, defaultAddress)
        }

        verify("Service must fetch wallet for user with id") {
            val user = userService.findWithWallet(user.email)
            assertThat(user).isNotNull
            assertThat(user!!.wallet).isNotNull
            assertThat(user.wallet!!.hash).isEqualTo(defaultAddress)
            assertThat(user.wallet!!.currency).isEqualTo(Currency.EUR)
            assertThat(user.wallet!!.type).isEqualTo(WalletType.USER)
        }
    }

    @Test
    fun mustBeAbleToCreateWalletForUser() {
        suppose("Blockchain service successfully adds wallet") {
            Mockito.`when`(mockedBlockchainService.addWallet(defaultAddress, defaultPublicKey))
                    .thenReturn(defaultAddressHash)
        }

        verify("Service can create wallet for a user") {
            val request = WalletCreateRequest(defaultAddress, defaultPublicKey)
            val wallet = walletService.createUserWallet(user, request)
            assertThat(wallet.hash).isEqualTo(defaultAddressHash)
            assertThat(wallet.currency).isEqualTo(Currency.EUR)
            assertThat(wallet.type).isEqualTo(WalletType.USER)
            assertThat(wallet.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
        }
        verify("Wallet is assigned to the user") {
            val userWithWallet = userService.findWithWallet(user.email)
            assertThat(userWithWallet).isNotNull
            assertThat(userWithWallet!!.wallet).isNotNull
            assertThat(userWithWallet.wallet!!.hash).isEqualTo(defaultAddressHash)
        }
    }

    @Test
    fun mustBeAbleToCreateWalletForProject() {
        suppose("Project exists") {
            val organization = createOrganization("Org", user)
            testContext.project = createProject("Das project", organization, user)
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

            val projectWithWallet = optionalProjectWithWallet.get()
            assertThat(projectWithWallet.wallet).isNotNull
            assertThat(projectWithWallet.wallet!!.hash).isEqualTo(defaultAddressHash)
        }
    }

    @Test
    fun mustNotBeAbleToCreateMultipleWalletsForOneUser() {
        suppose("User has a wallet") {
            createWalletForUser(user, defaultAddressHash)
        }

        verify("Service cannot create additional account") {
            val exception = assertThrows<ResourceAlreadyExistsException> {
                val request = WalletCreateRequest(defaultAddress, defaultPublicKey)
                walletService.createUserWallet(user, request)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_EXISTS)
        }
    }

    @Test
    fun mustNotBeAbleToCreateMultipleWalletsForOneProject() {
        suppose("Project exists") {
            val organization = createOrganization("Org", user)
            testContext.project = createProject("Das project", organization, user)
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
            createWalletForUser(user, defaultAddressHash)
        }
        suppose("User has some funds on a wallet") {
            testContext.balance = 100
            Mockito.`when`(mockedBlockchainService.getBalance(defaultAddressHash)).thenReturn(testContext.balance)
        }

        verify("Service can return wallet balance") {
            val balance = walletService.getWalletBalance(user.wallet!!)
            assertThat(balance).isEqualTo(testContext.balance)
        }
    }

    @Test
    fun mustThrowExceptionIfUserWithoutWalletTriesToGenerateCreateProjectWallet() {
        suppose("Project exists") {
            val organization = createOrganization("Org", user)
            testContext.project = createProject("Das project", organization, user)
        }

        verify("Service will throw InternalException") {
            val exception = assertThrows<ResourceNotFoundException> {
                walletService.generateTransactionToCreateProjectWallet(testContext.project)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_MISSING)
        }
    }

    @Test
    fun mustThrowExceptionIfOrganizationWithoutWalletTriesToGenerateCreateProjectWallet() {
        suppose("User has a wallet") {
            createWalletForUser(user, defaultAddressHash)
        }
        suppose("Project exists") {
            val organization = createOrganization("Org", user)
            testContext.project = createProject("Das project", organization, user)
        }

        verify("Service will throw InternalException") {
            val exception = assertThrows<ResourceNotFoundException> {
                walletService.generateTransactionToCreateProjectWallet(testContext.project)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_MISSING)
        }
    }

    @Test
    fun mustThrowExceptionWhenGenerateTransactionToCreateOrganizationWalletWithoutUserWallet() {
        suppose("Organization exists") {
            testContext.organization = createOrganization("Org", user)
        }

        verify("Service can generate create organization transaction") {
            val exception = assertThrows<ResourceNotFoundException> {
                walletService.generateTransactionToCreateOrganizationWallet(testContext.organization)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_MISSING)
        }
    }

    @Test
    fun mustThrowExceptionIfOrganizationAlreadyHasWallet() {
        suppose("Organization exists") {
            testContext.organization = createOrganization("Org", user)
        }
        suppose("Organization has a wallet") {
            createWalletForOrganization(testContext.organization, defaultAddressHash)
        }

        verify("Service will throw exception that organization already has a wallet") {
            val exception = assertThrows<ResourceAlreadyExistsException> {
                walletService.generateTransactionToCreateOrganizationWallet(testContext.organization)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_EXISTS)
        }
    }

    @Test
    fun mustGenerateTransactionToCreateOrganizationWallet() {
        suppose("User has a wallet") {
            testContext.wallet = createWalletForUser(user, defaultAddress)
        }
        suppose("Organization exists") {
            testContext.organization = createOrganization("Org", user)
        }
        suppose("Blockchain service will generate transaction") {
            Mockito.`when`(
                    mockedBlockchainService.generateAddOrganizationTransaction(
                            testContext.wallet.hash, testContext.organization.name)
            ).thenReturn(defaultTransactionData)
        }

        verify("Service can generate transaction") {
            val transaction = walletService.generateTransactionToCreateOrganizationWallet(testContext.organization)
            assertThat(transaction).isEqualTo(defaultTransactionData)
        }
    }

    @Test
    fun mustBeAbleToCreateOrganizationWallet() {
        suppose("Organization exists") {
            testContext.organization = createOrganization("Org", user)
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

            val organization = optionalOrganization.get()
            assertThat(organization.wallet).isNotNull
            assertThat(organization.wallet!!.hash).isEqualTo(defaultAddressHash)
        }
    }

    @Test
    fun mustThrowExceptionForCreateOrganizationWalletIfOrganizationAlreadyHasWallet() {
        suppose("Organization exists") {
            testContext.organization = createOrganization("Org", user)
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
            createWalletForUser(user, defaultAddressHash)
        }
        suppose("Project exists") {
            val organization = createOrganization("Org", user)
            testContext.project = createProject("Das project", organization, user)
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

    private class TestContext {
        lateinit var organization: Organization
        lateinit var project: Project
        lateinit var wallet: Wallet
        var balance: Long = -1
    }
}
