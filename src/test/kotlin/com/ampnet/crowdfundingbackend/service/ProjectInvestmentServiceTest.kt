package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.blockchain.pojo.ProjectInvestmentTxRequest
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.InvalidRequestException
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.service.impl.ProjectInvestmentServiceImpl
import com.ampnet.crowdfundingbackend.service.impl.TransactionInfoServiceImpl
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
        val transactionService = TransactionInfoServiceImpl(transactionInfoRepository)
        val walletService = WalletServiceImpl(walletRepository, userRepository, projectRepository,
                organizationRepository, mockedBlockchainService, transactionService)
        ProjectInvestmentServiceImpl(walletService, mockedBlockchainService, transactionService)
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
            testContext.investmentRequest = ProjectInvestmentRequest(testContext.project, user, 100)
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
            testContext.investmentRequest = ProjectInvestmentRequest(testContext.project, user, 100)
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
            testContext.investmentRequest = ProjectInvestmentRequest(testContext.project, user, 10)
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
            testContext.investmentRequest = ProjectInvestmentRequest(testContext.project, user, 10_000)
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
            val investmentRequest = ProjectInvestmentRequest(testContext.project, user, 100)
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
            val investmentRequest = ProjectInvestmentRequest(testContext.project, user, 100)
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
            testContext.investmentRequest = ProjectInvestmentRequest(testContext.project, user, 100)
            val userWalletHash = getWalletHash(user.wallet)
            Mockito.`when`(mockedBlockchainService.getBalance(userWalletHash)).thenReturn(10)
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
            testContext.investmentRequest = ProjectInvestmentRequest(testContext.project, user, 100_00)
            val userWalletHash = getWalletHash(user.wallet)
            Mockito.`when`(mockedBlockchainService.getBalance(userWalletHash)).thenReturn(100_000_00)
        }
        suppose("Project has empty wallet") {
            testContext.project.wallet =
                    createWalletForProject(testContext.project, testContext.defaultProjectAddressHash)
            val projectWalletHash = getWalletHash(testContext.project.wallet)
            Mockito.`when`(mockedBlockchainService.getBalance(projectWalletHash)).thenReturn(0)
        }
        suppose("Blockchain service will generate transaction") {
            val userWalletHash = getWalletHash(user.wallet)
            val projectWalletHash = getWalletHash(testContext.project.wallet)
            Mockito.`when`(mockedBlockchainService.generateProjectInvestmentTransaction(
                ProjectInvestmentTxRequest(userWalletHash, projectWalletHash, 100_00))
            ).thenReturn(testContext.defaultTransactionData)
        }

        verify("Service will generate transaction") {
            val transactionData = projectInvestmentService
                .generateInvestInProjectTransaction(testContext.investmentRequest)
            assertThat(transactionData.transactionData).isEqualTo(testContext.defaultTransactionData)
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
            testContext.investmentRequest = ProjectInvestmentRequest(testContext.project, user, 100_00)
            val userWalletHash = getWalletHash(user.wallet)
            Mockito.`when`(mockedBlockchainService.getBalance(userWalletHash)).thenReturn(100_000_00)
        }
        suppose("Project wallet has expected funding") {
            testContext.project.wallet =
                createWalletForProject(testContext.project, testContext.defaultProjectAddressHash)
            val projectWalletHash = getWalletHash(testContext.project.wallet)
            Mockito.`when`(mockedBlockchainService.getBalance(projectWalletHash)).thenReturn(10_000_000)
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
            Mockito.`when`(mockedBlockchainService
                    .postTransaction(testContext.defaultSignedTransaction, PostTransactionType.PRJ_INVEST)
            ).thenReturn(testContext.defaultTxHash)
        }

        verify("Service can post project invest transaction") {
            val txHash = projectInvestmentService.investInProject(testContext.defaultSignedTransaction)
            assertThat(txHash).isEqualTo(testContext.defaultTxHash)
        }
    }

    @Test
    fun mustNotBeAbleToGenerateConfirmInvestmentWithoutUserWallet() {
        suppose("Project exists") {
            databaseCleanerService.deleteAllProjects()
            testContext.project = createProject("test name", organization, user)
        }

        verify("Service will throw exception that user wallet is missing") {
            val exception = assertThrows<ResourceNotFoundException> {
                projectInvestmentService.generateConfirmInvestment(user, testContext.project)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_MISSING)
        }
    }

    @Test
    fun mustNotBeAbleToGenerateConfirmInvestmentWithoutProjectWallet() {
        suppose("Project exists") {
            databaseCleanerService.deleteAllProjects()
            testContext.project = createProject("test name", organization, user)
        }
        suppose("User has a wallet") {
            createWalletForUser(user, testContext.defaultAddressHash)
        }

        verify("Service will throw exception that project wallet is missing") {
            val exception = assertThrows<ResourceNotFoundException> {
                projectInvestmentService.generateConfirmInvestment(user, testContext.project)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_MISSING)
        }
    }

    @Test
    fun mustBeAbleGenerateConfirmInvestment() {
        suppose("Project exists") {
            databaseCleanerService.deleteAllProjects()
            testContext.project = createProject("test name", organization, user)
        }
        suppose("User has a wallet") {
            createWalletForUser(user, testContext.defaultAddressHash)
        }
        suppose("Project has wallet") {
            testContext.project.wallet =
                createWalletForProject(testContext.project, testContext.defaultProjectAddressHash)
        }
        suppose("Blockchain service will generate transaction") {
            val userWalletHash = getWalletHash(user.wallet)
            val projectWalletHash = getWalletHash(testContext.project.wallet)
            Mockito.`when`(
                    mockedBlockchainService.generateConfirmInvestment(userWalletHash, projectWalletHash)
            ).thenReturn(testContext.defaultTransactionData)
        }

        verify("Service will generate confirm transaction") {
            val transactionData = projectInvestmentService.generateConfirmInvestment(user, testContext.project)
            assertThat(transactionData.transactionData).isEqualTo(testContext.defaultTransactionData)
        }
    }

    private class TestContext {
        lateinit var project: Project
        lateinit var investmentRequest: ProjectInvestmentRequest
        val defaultAddressHash = "0x4e4ee58ff3a9e9e78c2dfdbac0d1518e4e1039f9189267e1dc8d3e35cbdf7892"
        val defaultProjectAddressHash = "0x1e4ee58ff3a9e9e78c2dfdbac32133e4e1039f9189267e1dc8d3e35cbdf7111"
        val defaultSignedTransaction = "SignedTransactionRequest"
        val defaultTransactionData = TransactionData("data", "to", 1, 1, 1, 1, "public_key")
        val defaultTxHash = "0x5432jlhkljkhsf78y7y23rekljhjksadhf6t4632ilhasdfh7836242hluafhds"
    }
}
