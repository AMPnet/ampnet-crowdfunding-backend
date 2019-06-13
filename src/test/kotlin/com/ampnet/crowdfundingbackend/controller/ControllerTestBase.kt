package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.TestBase
import com.ampnet.crowdfundingbackend.blockchain.BlockchainService
import com.ampnet.crowdfundingbackend.config.DatabaseCleanerService
import com.ampnet.crowdfundingbackend.enums.AuthMethod
import com.ampnet.crowdfundingbackend.enums.Currency
import com.ampnet.crowdfundingbackend.enums.OrganizationRoleType
import com.ampnet.crowdfundingbackend.enums.UserRoleType
import com.ampnet.crowdfundingbackend.enums.WalletType
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.ErrorResponse
import com.ampnet.crowdfundingbackend.persistence.model.Document
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.OrganizationMembership
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.persistence.model.UserWallet
import com.ampnet.crowdfundingbackend.persistence.model.Wallet
import com.ampnet.crowdfundingbackend.persistence.repository.DocumentRepository
import com.ampnet.crowdfundingbackend.persistence.repository.OrganizationInviteRepository
import com.ampnet.crowdfundingbackend.persistence.repository.OrganizationMembershipRepository
import com.ampnet.crowdfundingbackend.persistence.repository.OrganizationRepository
import com.ampnet.crowdfundingbackend.persistence.repository.ProjectRepository
import com.ampnet.crowdfundingbackend.persistence.repository.RoleRepository
import com.ampnet.crowdfundingbackend.persistence.repository.TransactionInfoRepository
import com.ampnet.crowdfundingbackend.persistence.repository.UserRepository
import com.ampnet.crowdfundingbackend.persistence.repository.UserWalletRepository
import com.ampnet.crowdfundingbackend.persistence.repository.WalletRepository
import com.ampnet.crowdfundingbackend.service.CloudStorageService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.fail
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.restdocs.RestDocumentationContextProvider
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation
import org.springframework.restdocs.operation.preprocess.Preprocessors
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.ZonedDateTime

@ExtendWith(value = [SpringExtension::class, RestDocumentationExtension::class])
@SpringBootTest
@ActiveProfiles("MailMockConfig, BlockchainServiceMockConfig, CloudStorageMockConfig")
abstract class ControllerTestBase : TestBase() {

    protected val defaultEmail = "user@email.com"
    protected val userUuid = "1234-1234-1234-1234"

    @Autowired
    protected lateinit var objectMapper: ObjectMapper
    @Autowired
    protected lateinit var databaseCleanerService: DatabaseCleanerService
    @Autowired
    protected lateinit var userRepository: UserRepository
    @Autowired
    protected lateinit var roleRepository: RoleRepository
    @Autowired
    protected lateinit var walletRepository: WalletRepository
    @Autowired
    protected lateinit var projectRepository: ProjectRepository
    @Autowired
    protected lateinit var organizationRepository: OrganizationRepository
    @Autowired
    private lateinit var membershipRepository: OrganizationMembershipRepository
    @Autowired
    protected lateinit var blockchainService: BlockchainService
    @Autowired
    protected lateinit var transactionInfoRepository: TransactionInfoRepository
    @Autowired
    protected lateinit var cloudStorageService: CloudStorageService
    @Autowired
    protected lateinit var organizationInviteRepository: OrganizationInviteRepository
    @Autowired
    protected lateinit var userWalletRepository: UserWalletRepository
    @Autowired
    private lateinit var documentRepository: DocumentRepository

    protected lateinit var mockMvc: MockMvc

    @BeforeEach
    fun init(wac: WebApplicationContext, restDocumentation: RestDocumentationContextProvider) {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply<DefaultMockMvcBuilder>(SecurityMockMvcConfigurers.springSecurity())
                .apply<DefaultMockMvcBuilder>(MockMvcRestDocumentation.documentationConfiguration(restDocumentation))
                .alwaysDo<DefaultMockMvcBuilder>(MockMvcRestDocumentation.document(
                        "{ClassName}/{methodName}",
                        Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
                        Preprocessors.preprocessResponse(Preprocessors.prettyPrint())
                ))
                .build()
    }

    protected fun getResponseErrorCode(errorCode: ErrorCode): String {
        return errorCode.categoryCode + errorCode.specificCode
    }

    protected fun verifyResponseErrorCode(result: MvcResult, errorCode: ErrorCode) {
        val response: ErrorResponse = objectMapper.readValue(result.response.contentAsString)
        val expectedErrorCode = getResponseErrorCode(errorCode)
        assert(response.errCode == expectedErrorCode)
    }

    protected fun createUser(email: String): User {
        val user = User::class.java.getConstructor().newInstance()
        user.authMethod = AuthMethod.EMAIL
        user.createdAt = ZonedDateTime.now()
        user.email = email
        user.enabled = true
        user.firstName = "First"
        user.lastName = "Last"
        user.role = roleRepository.getOne(UserRoleType.USER.id)
        return userRepository.save(user)
    }

    protected fun createWalletForUser(userUuid: String, hash: String): Wallet {
        val wallet = createWallet(hash, WalletType.USER)
        val userWallet = UserWallet(0, userUuid, wallet)
        userWalletRepository.save(userWallet)
        return wallet
    }

    protected fun createWalletForProject(project: Project, address: String): Wallet {
        val wallet = createWallet(address, WalletType.PROJECT)
        project.wallet = wallet
        projectRepository.save(project)
        return wallet
    }

    protected fun createWalletForOrganization(organization: Organization, hash: String): Wallet {
        val wallet = createWallet(hash, WalletType.ORG)
        organization.wallet = wallet
        organizationRepository.save(organization)
        return wallet
    }

    private fun createWallet(hash: String, type: WalletType): Wallet {
        val wallet = Wallet::class.java.getConstructor().newInstance()
        wallet.hash = hash
        wallet.type = type
        wallet.currency = Currency.EUR
        wallet.createdAt = ZonedDateTime.now()
        return walletRepository.save(wallet)
    }

    protected fun createOrganization(name: String, userUuid: String): Organization {
        val organization = Organization::class.java.getConstructor().newInstance()
        organization.name = name
        organization.legalInfo = "some legal info"
        organization.createdAt = ZonedDateTime.now()
        organization.approved = true
        organization.createdByUserUuid = userUuid
        organization.documents = emptyList()
        return organizationRepository.save(organization)
    }

    protected fun addUserToOrganization(userUuid: String, organizationId: Int, role: OrganizationRoleType) {
        val membership = OrganizationMembership::class.java.getConstructor().newInstance()
        membership.userUuid = userUuid
        membership.organizationId = organizationId
        membership.role = roleRepository.getOne(role.id)
        membership.createdAt = ZonedDateTime.now()
        membershipRepository.save(membership)
    }

    protected fun createProject(
        name: String,
        organization: Organization,
        createdBy: User,
        active: Boolean = true,
        startDate: ZonedDateTime = ZonedDateTime.now(),
        endDate: ZonedDateTime = ZonedDateTime.now().plusDays(30),
        expectedFunding: Long = 10_000_000,
        minPerUser: Long = 10,
        maxPerUser: Long = 10_000
    ): Project {
        val project = Project::class.java.newInstance()
        project.organization = organization
        project.name = name
        project.description = "description"
        project.location = "location"
        project.locationText = "locationText"
        project.returnOnInvestment = "0-1%"
        project.startDate = startDate
        project.endDate = endDate
        project.expectedFunding = expectedFunding
        project.currency = Currency.EUR
        project.minPerUser = minPerUser
        project.maxPerUser = maxPerUser
        project.createdBy = createdBy
        project.active = active
        project.createdAt = startDate.minusMinutes(1)
        return projectRepository.save(project)
    }

    protected fun saveDocument(
        name: String,
        link: String,
        type: String,
        size: Int,
        createdByUserUuid: String
    ): Document {
        val document = Document::class.java.getDeclaredConstructor().newInstance()
        document.name = name
        document.link = link
        document.type = type
        document.size = size
        document.createdByUserUuid = createdByUserUuid
        document.createdAt = ZonedDateTime.now()
        return documentRepository.save(document)
    }

    protected fun getUserWalletHash(userUuid: String): String {
        val optionalUserWallet = userWalletRepository.findByUserUuid(userUuid)
        assertThat(optionalUserWallet).isPresent
        return optionalUserWallet.get().wallet.hash
    }

    protected fun getWalletHash(wallet: Wallet?): String = wallet?.hash ?: fail("User wallet must not be null")
}
