package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.TestBase
import com.ampnet.crowdfundingbackend.config.ApplicationProperties
import com.ampnet.crowdfundingbackend.config.DatabaseCleanerService
import com.ampnet.crowdfundingbackend.config.PasswordEncoderConfig
import com.ampnet.crowdfundingbackend.enums.OrganizationRoleType
import com.ampnet.crowdfundingbackend.enums.UserRoleType
import com.ampnet.crowdfundingbackend.enums.AuthMethod
import com.ampnet.crowdfundingbackend.enums.Currency
import com.ampnet.crowdfundingbackend.enums.TransactionType
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.OrganizationInvite
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.ProjectInvestment
import com.ampnet.crowdfundingbackend.persistence.model.Transaction
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.persistence.model.Wallet
import com.ampnet.crowdfundingbackend.persistence.repository.CountryRepository
import com.ampnet.crowdfundingbackend.persistence.repository.MailTokenRepository
import com.ampnet.crowdfundingbackend.persistence.repository.OrganizationFollowerRepository
import com.ampnet.crowdfundingbackend.persistence.repository.OrganizationInviteRepository
import com.ampnet.crowdfundingbackend.persistence.repository.OrganizationMembershipRepository
import com.ampnet.crowdfundingbackend.persistence.repository.OrganizationRepository
import com.ampnet.crowdfundingbackend.persistence.repository.ProjectInvestmentRepository
import com.ampnet.crowdfundingbackend.persistence.repository.ProjectRepository
import com.ampnet.crowdfundingbackend.persistence.repository.RoleRepository
import com.ampnet.crowdfundingbackend.persistence.repository.TransactionRepository
import com.ampnet.crowdfundingbackend.persistence.repository.UserRepository
import com.ampnet.crowdfundingbackend.persistence.repository.WalletRepository
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.ZonedDateTime

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
    protected lateinit var transactionRepository: TransactionRepository
    @Autowired
    protected lateinit var countryRepository: CountryRepository
    @Autowired
    protected lateinit var mailTokenRepository: MailTokenRepository
    @Autowired
    protected lateinit var projectRepository: ProjectRepository
    @Autowired
    protected lateinit var projectInvestmentRepository: ProjectInvestmentRepository

    protected val applicationProperties: ApplicationProperties by lazy {
        // add additional properties as needed
        val applicationProperties = ApplicationProperties()
        applicationProperties.mail.enabled = true
        applicationProperties
    }

    protected fun createUser(email: String, firstName: String, lastName: String): User {
        val user = User::class.java.newInstance()
        user.authMethod = AuthMethod.EMAIL
        user.createdAt = ZonedDateTime.now()
        user.email = email
        user.enabled = true
        user.firstName = firstName
        user.lastName = lastName
        user.role = roleRepository.getOne(UserRoleType.USER.id)
        return userRepository.save(user)
    }

    protected fun createOrganization(name: String, createdBy: User): Organization {
        val organization = Organization::class.java.newInstance()
        organization.name = name
        organization.legalInfo = "some legal info"
        organization.createdAt = ZonedDateTime.now()
        organization.approved = true
        organization.createdByUser = createdBy
        organization.documents = listOf("hash1", "hash2", "hash3")
        return organizationRepository.save(organization)
    }

    protected fun createWalletForUser(userId: Int): Wallet {
        val wallet = Wallet::class.java.newInstance()
        wallet.ownerId = userId
        wallet.currency = Currency.EUR
        wallet.transactions = emptyList()
        wallet.createdAt = ZonedDateTime.now()
        return walletRepository.save(wallet)
    }

    protected fun createOrganizationInvite(
        userId: Int,
        organizationId: Int,
        role: OrganizationRoleType,
        invitedBy: Int
    ): OrganizationInvite {
        val invite = OrganizationInvite::class.java.newInstance()
        invite.userId = userId
        invite.organizationId = organizationId
        invite.createdAt = ZonedDateTime.now()
        invite.role = roleRepository.getOne(role.id)
        invite.invitedBy = invitedBy
        return inviteRepository.save(invite)
    }

    protected fun createProject(
        name: String,
        organization: Organization,
        createdBy: User,
        active: Boolean = true,
        startDate: ZonedDateTime = ZonedDateTime.now(),
        endDate: ZonedDateTime = ZonedDateTime.now().plusDays(30),
        expectedFunding: BigDecimal = BigDecimal(10_000_000),
        minPerUser: BigDecimal = BigDecimal(10),
        maxPerUser: BigDecimal = BigDecimal(10_000)
    ): Project {
        val project = Project::class.java.newInstance()
        project.organization = organization
        project.name = name
        project.description = "description"
        project.location = "location"
        project.locationText = "locationText"
        project.returnToInvestment = "0-1%"
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

    protected fun createProjectInvestment(
        user: User,
        walletId: Int,
        project: Project,
        amount: BigDecimal,
        txHash: String = "hash"
    ): ProjectInvestment {
        val investment = ProjectInvestment::class.java.newInstance()
        investment.user = user
        investment.project = project
        investment.transaction = createTransaction(
                walletId, user.getFullName(), project.name, amount, txHash, TransactionType.TRANSFER)
        return projectInvestmentRepository.save(investment)
    }

    protected fun createTransaction(
        walletId: Int,
        sender: String,
        receiver: String,
        amount: BigDecimal,
        txHash: String,
        type: TransactionType
    ): Transaction {
        val transaction = Transaction::class.java.newInstance()
        transaction.timestamp = ZonedDateTime.now()
        transaction.currency = Currency.EUR
        transaction.walletId = walletId
        transaction.sender = sender
        transaction.receiver = receiver
        transaction.amount = amount
        transaction.txHash = txHash
        transaction.type = type
        return transactionRepository.save(transaction)
    }
}
