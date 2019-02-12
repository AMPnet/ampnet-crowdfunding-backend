package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.enums.Currency
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.InvalidRequestException
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.service.impl.ProjectInvestmentServiceImpl
import com.ampnet.crowdfundingbackend.service.impl.WalletServiceImpl
import com.ampnet.crowdfundingbackend.service.pojo.PostTransactionType
import com.ampnet.crowdfundingbackend.service.pojo.ProjectInvestmentRequest
import com.ampnet.crowdfundingbackend.service.pojo.TransactionData
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.time.ZonedDateTime

class ProjectInvestmentServiceTest : JpaServiceTestBase() {

    private val projectInvestmentService: ProjectInvestmentService by lazy {
        val walletService = WalletServiceImpl(
                walletRepository, userRepository, projectRepository, organizationRepository,
                walletTokenRepository, mockedBlockchainService
        )
        ProjectInvestmentServiceImpl(walletService, mockedBlockchainService)
    }
    private val organization: Organization by lazy {
        databaseCleanerService.deleteAllOrganizations()
        createOrganization("Das Organization", user)
    }
    private lateinit var user: User
    private lateinit var testContext: TestContext

    @BeforeEach
    fun initTestContext() {
        databaseCleanerService.deleteAllWalletsAndOwners()
        user = createUser("test@email.com", "First", "Last")
        testContext = TestContext()
    }

    @Test
    fun mustThrowExceptionIfProjectIsNotActive() {
        suppose("Project is not active") {
            databaseCleanerService.deleteAllProjects()
            testContext.project = createProject("test name", organization, user, false)
        }
        suppose("Request is for inactive project") {
            testContext.investmentRequest = ProjectInvestmentRequest(
                    testContext.project, user, 100, Currency.EUR)
        }

        verify("Service will throw exception project not active") {
            val exception = assertThrows<InvalidRequestException> {
                projectInvestmentService.generateInvestInProjectTransaction(testContext.investmentRequest)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.PRJ_NOT_ACTIVE)
        }
    }

    @Test
    fun mustThrowExceptionIfProjectHasExpired() {
        suppose("Project has expired") {
            databaseCleanerService.deleteAllProjects()
            testContext.project = createProject("test name", organization, user,
                    startDate = ZonedDateTime.now().minusDays(30),
                    endDate = ZonedDateTime.now().minusDays(2)
            )
        }
        suppose("Request is for expired project") {
            testContext.investmentRequest = ProjectInvestmentRequest(
                    testContext.project, user, 100, Currency.EUR)
        }

        verify("Service will throw exception project expired") {
            val exception = assertThrows<InvalidRequestException> {
                projectInvestmentService.generateInvestInProjectTransaction(testContext.investmentRequest)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.PRJ_DATE_EXPIRED)
        }
    }

    @Test
    fun mustThrowExceptionIfInvestmentAmountIsBelowMinimum() {
        suppose("Project exists") {
            databaseCleanerService.deleteAllProjects()
            testContext.project = createProject("test name", organization, user, minPerUser = 100)
        }
        suppose("Request amount is below project minimum") {
            testContext.investmentRequest = ProjectInvestmentRequest(testContext.project, user, 10, Currency.EUR)
        }

        verify("Service will throw exception investment below project minimum") {
            val exception = assertThrows<InvalidRequestException> {
                projectInvestmentService.generateInvestInProjectTransaction(testContext.investmentRequest)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.PRJ_MIN_PER_USER)
        }
    }

    @Test
    fun mustThrowExceptionIfInvestmentAmountIsAboveMaximum() {
        suppose("Project exists") {
            databaseCleanerService.deleteAllProjects()
            testContext.project = createProject("test name", organization, user, maxPerUser = 1_000)
        }
        suppose("Request amount is about project maximum") {
            testContext.investmentRequest = ProjectInvestmentRequest(testContext.project, user, 10_000, Currency.EUR)
        }

        verify("Service will throw exception investment below project minimum") {
            val exception = assertThrows<InvalidRequestException> {
                projectInvestmentService.generateInvestInProjectTransaction(testContext.investmentRequest)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.PRJ_MAX_PER_USER)
        }
    }

    @Test
    fun mustThrowExceptionIfUserDoesNotHaveWallet() {
        suppose("Project exists") {
            databaseCleanerService.deleteAllProjects()
            testContext.project = createProject("test name", organization, user)
        }

        verify("Service will throw exception that user wallet is missing") {
            val investmentRequest = ProjectInvestmentRequest(testContext.project, user, 100, Currency.EUR)
            val exception = assertThrows<ResourceNotFoundException> {
                projectInvestmentService.generateInvestInProjectTransaction(investmentRequest)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_MISSING)
        }
    }

    @Test
    fun mustThrowExceptionIfProjectDoesNotHaveWallet() {
        suppose("Project exists") {
            databaseCleanerService.deleteAllProjects()
            testContext.project = createProject("test name", organization, user)
        }
        suppose("User has a wallet") {
            createWalletForUser(user, testContext.defaultAddressHash)
        }
        suppose("User has enough funds") {
            Mockito.`when`(mockedBlockchainService.getBalance(testContext.defaultAddressHash)).thenReturn(100_000_00)
        }

        verify("Service will throw exception that project wallet is missing") {
            val investmentRequest = ProjectInvestmentRequest(
                    testContext.project, user, 100, Currency.EUR)
            val exception = assertThrows<ResourceNotFoundException> {
                projectInvestmentService.generateInvestInProjectTransaction(investmentRequest)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_MISSING)
        }
    }

    @Test
    fun mustThrowExceptionIfUserDoesNotEnoughFunds() {
        suppose("Project exists") {
            databaseCleanerService.deleteAllProjects()
            testContext.project = createProject("test name", organization, user)
        }
        suppose("User has a wallet") {
            createWalletForUser(user, testContext.defaultAddressHash)
        }
        suppose("User does not have enough funds on wallet") {
            testContext.investmentRequest = ProjectInvestmentRequest(
                    testContext.project, user, 100, Currency.EUR)
            Mockito.`when`(mockedBlockchainService.getBalance(user.wallet!!.hash)).thenReturn(10)
        }

        verify("Service will throw exception investment below project minimum") {
            val exception = assertThrows<InvalidRequestException> {
                projectInvestmentService.generateInvestInProjectTransaction(testContext.investmentRequest)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_FUNDS)
        }
    }

    @Test
    fun mustBeAbleToGenerateInvestment() {
        suppose("Project exists") {
            databaseCleanerService.deleteAllProjects()
            testContext.project = createProject("test name", organization, user)
        }
        suppose("User has a wallet") {
            createWalletForUser(user, testContext.defaultAddressHash)
        }
        suppose("User does have enough funds on wallet") {
            testContext.investmentRequest = ProjectInvestmentRequest(
                testContext.project, user, 100_00, Currency.EUR)
            Mockito.`when`(mockedBlockchainService.getBalance(user.wallet!!.hash)).thenReturn(100_000_00)
        }
        suppose("Project has empty wallet") {
            testContext.project.wallet =
                createWalletForProject(testContext.project, testContext.defaultProjectAddressHash)
            Mockito.`when`(mockedBlockchainService.getBalance(testContext.project.wallet!!.hash)).thenReturn(0)
        }
        suppose("Blockchain service will generate transaction") {
            Mockito.`when`(mockedBlockchainService.generateInvestInProjectTransaction(
                user.wallet!!.hash, testContext.project.wallet!!.hash, 100_00)
            ).thenReturn(testContext.defaultTransactionData)
        }

        verify("Service will generate transaction") {
            val transactionData = projectInvestmentService
                .generateInvestInProjectTransaction(testContext.investmentRequest)
            assertThat(transactionData).isEqualTo(testContext.defaultTransactionData)
        }
    }

    @Test
    fun mustNotBeAbleToGenerateInvestmentIfProjectDidReachExpectedFunding() {
        suppose("Project exists") {
            databaseCleanerService.deleteAllProjects()
            testContext.project = createProject("test name", organization, user)
        }
        suppose("User has a wallet") {
            createWalletForUser(user, testContext.defaultAddressHash)
        }
        suppose("User does have enough funds on wallet") {
            testContext.investmentRequest = ProjectInvestmentRequest(
                testContext.project, user, 100_00, Currency.EUR)
            Mockito.`when`(mockedBlockchainService.getBalance(user.wallet!!.hash)).thenReturn(100_000_00)
        }
        suppose("Project wallet has expected funding") {
            testContext.project.wallet =
                createWalletForProject(testContext.project, testContext.defaultProjectAddressHash)
            Mockito.`when`(mockedBlockchainService.getBalance(testContext.project.wallet!!.hash)).thenReturn(10_000_000)
        }

        verify("Service will throw exception") {
            val exception = assertThrows<InvalidRequestException> {
                projectInvestmentService.generateInvestInProjectTransaction(testContext.investmentRequest)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.PRJ_MAX_FUNDS)
        }
    }

    @Test
    fun mustBeAbleInvestInProject() {
        suppose("Blockchain service will return hash for post transaction") {
            Mockito.`when`(
                mockedBlockchainService
                    .postTransaction(testContext.defaultSignedTransaction, PostTransactionType.PRJ_INVEST)
            ).thenReturn(testContext.defaultTxHash)
        }

        verify("Service can post project invest transaction") {
            val txHash = projectInvestmentService.investInProject(testContext.defaultSignedTransaction)
            assertThat(txHash).isEqualTo(testContext.defaultTxHash)
        }
    }

    private class TestContext {
        lateinit var project: Project
        lateinit var investmentRequest: ProjectInvestmentRequest
        val defaultAddressHash = "0x4e4ee58ff3a9e9e78c2dfdbac0d1518e4e1039f9189267e1dc8d3e35cbdf7892"
        val defaultProjectAddressHash = "0x1e4ee58ff3a9e9e78c2dfdbac32133e4e1039f9189267e1dc8d3e35cbdf7111"
        val defaultSignedTransaction = "SignedTransaction"
        val defaultTransactionData = TransactionData("data", "to", 1, 1, 1, 1, "public_key")
        val defaultTxHash = "0x5432jlhkljkhsf78y7y23rekljhjksadhf6t4632ilhasdfh7836242hluafhds"
    }
}
