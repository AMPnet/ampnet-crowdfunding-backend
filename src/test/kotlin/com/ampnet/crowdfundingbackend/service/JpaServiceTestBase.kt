package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.TestBase
import com.ampnet.crowdfundingbackend.config.ApplicationProperties
import com.ampnet.crowdfundingbackend.config.DatabaseCleanerService
import com.ampnet.crowdfundingbackend.config.PasswordEncoderConfig
import com.ampnet.crowdfundingbackend.enums.OrganizationRoleType
import com.ampnet.crowdfundingbackend.enums.UserRoleType
import com.ampnet.crowdfundingbackend.enums.AuthMethod
import com.ampnet.crowdfundingbackend.enums.Currency
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.OrganizationInvite
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.persistence.model.Wallet
import com.ampnet.crowdfundingbackend.persistence.repository.CountryRepository
import com.ampnet.crowdfundingbackend.persistence.repository.MailTokenRepository
import com.ampnet.crowdfundingbackend.persistence.repository.OrganizationFollowerRepository
import com.ampnet.crowdfundingbackend.persistence.repository.OrganizationInviteRepository
import com.ampnet.crowdfundingbackend.persistence.repository.OrganizationMembershipRepository
import com.ampnet.crowdfundingbackend.persistence.repository.OrganizationRepository
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
}
