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
import com.ampnet.crowdfundingbackend.service.pojo.ProjectInvestmentRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.ZonedDateTime

class ProjectInvestmentServiceTest : JpaServiceTestBase() {

    private val projectInvestmentService: ProjectInvestmentService by lazy {
        val walletService = WalletServiceImpl(
                walletRepository, userRepository, projectRepository, organizationRepository,
                walletTokenRepository, mockedBlockchainService
        )
        ProjectInvestmentServiceImpl(walletService)
    }
    private val user: User by lazy {
        databaseCleanerService.deleteAllUsers()
        createUser("test@email.com", "Test", "User")
    }
    private val organization: Organization by lazy {
        databaseCleanerService.deleteAllOrganizations()
        createOrganization("Das Organization", user)
    }
    private lateinit var testContext: TestContext

    @BeforeEach
    fun initTestContext() {
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
                projectInvestmentService.investToProject(testContext.investmentRequest)
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
                projectInvestmentService.investToProject(testContext.investmentRequest)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.PRJ_DATE_EXPIRED)
        }
    }

    @Test
    fun mustThrowExceptionIfInvestmentAmountIsBelowMinimum() {
        suppose("Project exists") {
            databaseCleanerService.deleteAllProjects()
            testContext.project = createProject("test name", organization, user,
                    minPerUser = 100
            )
        }
        suppose("Request amount is below project minimum") {
            testContext.investmentRequest = ProjectInvestmentRequest(
                    testContext.project, user, 10, Currency.EUR)
        }

        verify("Service will throw exception investment below project minimum") {
            val exception = assertThrows<InvalidRequestException> {
                projectInvestmentService.investToProject(testContext.investmentRequest)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.PRJ_MIN_PER_USER)
        }
    }

    @Test
    fun mustThrowExceptionIfInvestmentAmountIsAboveMaximum() {
        suppose("Project exists") {
            databaseCleanerService.deleteAllProjects()
            testContext.project = createProject("test name", organization, user,
                    maxPerUser = 1_000
            )
        }
        suppose("Request amount is about project maximum") {
            testContext.investmentRequest = ProjectInvestmentRequest(
                    testContext.project, user, 10_000, Currency.EUR)
        }

        verify("Service will throw exception investment below project minimum") {
            val exception = assertThrows<InvalidRequestException> {
                projectInvestmentService.investToProject(testContext.investmentRequest)
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
            val investmentRequest = ProjectInvestmentRequest(
                    testContext.project, user, 100, Currency.EUR)
            val exception = assertThrows<ResourceNotFoundException> {
                projectInvestmentService.investToProject(investmentRequest)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_MISSING)
        }
    }

    @Disabled
    @Test
    fun mustThrowExceptionIfProjectDoesNotHaveWallet() {
        suppose("Project exists") {
            databaseCleanerService.deleteAllProjects()
            testContext.project = createProject("test name", organization, user)
        }
        suppose("User has a wallet") {
            // TODO: create user wallet, delete wallets and refactor creating user because deleting wallets deletes users
        }

        verify("Service will throw exception that project wallet is missing") {
            val investmentRequest = ProjectInvestmentRequest(
                    testContext.project, user, 100, Currency.EUR)
            val exception = assertThrows<ResourceNotFoundException> {
                projectInvestmentService.investToProject(investmentRequest)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_MISSING)
        }
    }

    @Disabled("Define transactions")
    @Test
    fun mustThrowExceptionIfUserReachedMaximumFunding() {
        suppose("Project exists") {
            databaseCleanerService.deleteAllProjects()
            testContext.project = createProject("test name", organization, user, maxPerUser = 10_000)
        }
        suppose("User invested once") {
            // TODO: invest to project 9_000 greenars
        }
        suppose("Request amount is about maximum per user") {
            testContext.investmentRequest = ProjectInvestmentRequest(
                    testContext.project, user, 2_000, Currency.EUR)
        }

        verify("Service will throw exception investment below project minimum") {
            val exception = assertThrows<InvalidRequestException> {
                projectInvestmentService.investToProject(testContext.investmentRequest)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.PRJ_MAX_PER_USER)
        }
    }

    @Disabled("Create user wallet")
    @Test
    fun mustThrowExceptionIfUserDoesNotEnoughFunds() {
        suppose("Project exists") {
            databaseCleanerService.deleteAllProjects()
            testContext.project = createProject("test name", organization, user)
        }
        suppose("User does not have enough funds on wallet") {
            testContext.investmentRequest = ProjectInvestmentRequest(
                    testContext.project, user, 100, Currency.EUR)
            // TODO: mock blockchain service, current user funds are BigDecimal.ONE
        }

        verify("Service will throw exception investment below project minimum") {
            val exception = assertThrows<InvalidRequestException> {
                projectInvestmentService.investToProject(testContext.investmentRequest)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_FUNDS)
        }
    }

    @Disabled("Need blockchain service")
    @Test
    fun otherProjectInvestmentsMustNotBeCalculateToMaxPerUserFunding() {
        suppose("Project exists") {
            databaseCleanerService.deleteAllProjects()
            testContext.project = createProject("test name", organization, user,
                    maxPerUser = 10_000)
        }
        suppose("User invested once") {
            // TODO: invest to project
        }
        suppose("User invested in other project") {
//            val secondOrganization = createOrganization(UUID.randomUUID().toString(), user)
//            val secondProject = createProject(
//                    UUID.randomUUID().toString(), secondOrganization, user, maxPerUser = 10_000)
            // TODO: invest to second project
        }
        suppose("User has enough funds on wallet") {
            testContext.investmentRequest = ProjectInvestmentRequest(
                    testContext.project, user, 4_500, Currency.EUR)
            // TODO: mock blockchain
        }

        verify("Service will accept second investment") {
            projectInvestmentService.investToProject(testContext.investmentRequest)
        }
    }

    private class TestContext {
        lateinit var project: Project
        lateinit var investmentRequest: ProjectInvestmentRequest
    }
}
