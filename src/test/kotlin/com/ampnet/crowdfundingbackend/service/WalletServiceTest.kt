package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.exception.ResourceAlreadyExistsException
import com.ampnet.crowdfundingbackend.enums.Currency
import com.ampnet.crowdfundingbackend.enums.WalletType
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.InternalException
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.service.impl.UserServiceImpl
import com.ampnet.crowdfundingbackend.service.impl.WalletServiceImpl
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
        WalletServiceImpl(walletRepository, userRepository, projectRepository, mockedBlockchainService)
    }
    private lateinit var user: User
    private lateinit var testContext: TestContext

    private val defaultAddress = "0x14bC6a8219c798394726f8e86E040A878da1d99D"
    private val defaultAddressHash = "0x4e4ee58ff3a9e9e78c2dfdbac0d1518e4e1039f9189267e1dc8d3e35cbdf7892"
    private val defaultSignedTransaction = "SignedTransaction"

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
            Mockito.`when`(mockedBlockchainService.addWallet(defaultAddress)).thenReturn(defaultAddressHash)
        }

        verify("Service can create wallet for a user") {
            val wallet = walletService.createUserWallet(user, defaultAddress)
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
            Mockito.`when`(mockedBlockchainService.postTransaction(defaultSignedTransaction))
                    .thenReturn(defaultAddressHash)
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
                walletService.createUserWallet(user, defaultAddress)
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
    fun mustThrowExceptionIfBlockchainServiceFailsToCreateWallet() {
        suppose("gRPC service cannot create a wallet") {
            Mockito.`when`(mockedBlockchainService.addWallet(defaultAddress)).thenReturn(null)
        }

        verify("Service will throw InternalException") {
            val exception = assertThrows<InternalException> {
                walletService.createUserWallet(user, defaultAddress)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.INT_WALLET_ADD)
        }
    }

    @Test
    fun mustThrowExceptionIfBlockchainServiceToGetBalance() {
        suppose("User has a wallet") {
            createWalletForUser(user, defaultAddress)
        }
        suppose("gRPC service cannot get balance for wallet") {
            Mockito.`when`(mockedBlockchainService.getBalance(defaultAddress)).thenReturn(null)
        }

        verify("Service will throw InternalException") {
            val exception = assertThrows<InternalException> {
                walletService.getWalletBalance(user.wallet!!)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.INT_WALLET_FUNDS)
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

    private class TestContext {
        lateinit var project: Project
        var balance: Long = -1
    }
}
