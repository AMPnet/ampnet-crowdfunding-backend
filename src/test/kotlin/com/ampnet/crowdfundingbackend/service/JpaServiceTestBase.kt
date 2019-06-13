package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.TestBase
import com.ampnet.crowdfundingbackend.blockchain.BlockchainService
import com.ampnet.crowdfundingbackend.config.ApplicationProperties
import com.ampnet.crowdfundingbackend.config.DatabaseCleanerService
import com.ampnet.crowdfundingbackend.config.PasswordEncoderConfig
import com.ampnet.crowdfundingbackend.enums.AuthMethod
import com.ampnet.crowdfundingbackend.enums.Currency
import com.ampnet.crowdfundingbackend.enums.OrganizationRoleType
import com.ampnet.crowdfundingbackend.enums.UserRoleType
import com.ampnet.crowdfundingbackend.enums.WalletType
import com.ampnet.crowdfundingbackend.persistence.model.Document
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.OrganizationInvite
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.persistence.model.UserWallet
import com.ampnet.crowdfundingbackend.persistence.model.Wallet
import com.ampnet.crowdfundingbackend.persistence.repository.DocumentRepository
import com.ampnet.crowdfundingbackend.persistence.repository.OrganizationFollowerRepository
import com.ampnet.crowdfundingbackend.persistence.repository.OrganizationInviteRepository
import com.ampnet.crowdfundingbackend.persistence.repository.OrganizationMembershipRepository
import com.ampnet.crowdfundingbackend.persistence.repository.OrganizationRepository
import com.ampnet.crowdfundingbackend.persistence.repository.ProjectRepository
import com.ampnet.crowdfundingbackend.persistence.repository.RoleRepository
import com.ampnet.crowdfundingbackend.persistence.repository.TransactionInfoRepository
import com.ampnet.crowdfundingbackend.persistence.repository.UserRepository
import com.ampnet.crowdfundingbackend.persistence.repository.UserWalletRepository
import com.ampnet.crowdfundingbackend.persistence.repository.WalletRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.fail
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

@ExtendWith(SpringExtension::class)
@DataJpaTest
@Transactional(propagation = Propagation.SUPPORTS)
@Import(DatabaseCleanerService::class, PasswordEncoderConfig::class)
abstract class JpaServiceTestBase : TestBase() {

    @Autowired
    protected lateinit var databaseCleanerService: DatabaseCleanerService
    @Autowired
    protected lateinit var passwordEncoder: PasswordEncoder
    @Autowired
    protected lateinit var roleRepository: RoleRepository
    @Autowired
    protected lateinit var userRepository: UserRepository
    @Autowired
    protected lateinit var organizationRepository: OrganizationRepository
    @Autowired
    protected lateinit var membershipRepository: OrganizationMembershipRepository
    @Autowired
    protected lateinit var followerRepository: OrganizationFollowerRepository
    @Autowired
    protected lateinit var inviteRepository: OrganizationInviteRepository
    @Autowired
    protected lateinit var walletRepository: WalletRepository
    @Autowired
    protected lateinit var projectRepository: ProjectRepository
    @Autowired
    protected lateinit var documentRepository: DocumentRepository
    @Autowired
    protected lateinit var transactionInfoRepository: TransactionInfoRepository
    @Autowired
    protected lateinit var userWalletRepository: UserWalletRepository

    protected val mockedBlockchainService: BlockchainService = Mockito.mock(BlockchainService::class.java)

    protected val applicationProperties: ApplicationProperties by lazy {
        // add additional properties as needed
        val applicationProperties = ApplicationProperties()
        applicationProperties.mail.enabled = true
        applicationProperties
    }

    protected val userUuid = UUID.randomUUID().toString()

    protected fun createUser(email: String, firstName: String, lastName: String): User {
        val user = User::class.java.getConstructor().newInstance()
        user.authMethod = AuthMethod.EMAIL
        user.createdAt = ZonedDateTime.now()
        user.email = email
        user.enabled = true
        user.firstName = firstName
        user.lastName = lastName
        user.role = roleRepository.getOne(UserRoleType.USER.id)
        return userRepository.save(user)
    }

    protected fun createOrganization(name: String, createdByUuid: String): Organization {
        val organization = Organization::class.java.getConstructor().newInstance()
        organization.name = name
        organization.legalInfo = "some legal info"
        organization.createdAt = ZonedDateTime.now()
        organization.approved = true
        organization.createdByUserUuid = createdByUuid
        organization.documents = emptyList()
        return organizationRepository.save(organization)
    }

    protected fun createWalletForUser(userUuid: String, hash: String): Wallet {
        val wallet = createWallet(hash, WalletType.USER)
        val userWallet = UserWallet(0, userUuid, wallet)
        userWalletRepository.save(userWallet)
        return wallet
    }

    protected fun createWalletForProject(project: Project, hash: String): Wallet {
        val wallet = createWallet(hash, WalletType.PROJECT)
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

    protected fun createWallet(hash: String, type: WalletType): Wallet {
        val wallet = Wallet::class.java.getConstructor().newInstance()
        wallet.hash = hash
        wallet.type = type
        wallet.currency = Currency.EUR
        wallet.createdAt = ZonedDateTime.now()
        return walletRepository.save(wallet)
    }

    protected fun createOrganizationInvite(
        email: String,
        organizationId: Int,
        role: OrganizationRoleType,
        invitedByUuid: String
    ): OrganizationInvite {
        val invite = OrganizationInvite::class.java.getConstructor().newInstance()
        invite.email = email
        invite.organizationId = organizationId
        invite.createdAt = ZonedDateTime.now()
        invite.role = roleRepository.getOne(role.id)
        invite.invitedByUserUuid = invitedByUuid
        return inviteRepository.save(invite)
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
        createdByUserUuid: String,
        type: String = "document/type",
        size: Int = 100
    ): Document {
        val document = Document(0, link, name, type, size, createdByUserUuid, ZonedDateTime.now())
        return documentRepository.save(document)
    }

    protected fun getUserWalletHash(userUuid: String): String {
        val optionalUserWallet = userWalletRepository.findByUserUuid(userUuid)
        assertThat(optionalUserWallet).isPresent
        return optionalUserWallet.get().wallet.hash
    }

    protected fun getWalletHash(wallet: Wallet?): String = wallet?.hash ?: fail("User wallet must not be null")
}
