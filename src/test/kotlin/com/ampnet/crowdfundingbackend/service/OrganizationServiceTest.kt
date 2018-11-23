package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.TestBase
import com.ampnet.crowdfundingbackend.config.DatabaseCleanerService
import com.ampnet.crowdfundingbackend.enums.OrganizationRoleType
import com.ampnet.crowdfundingbackend.enums.UserRoleType
import com.ampnet.crowdfundingbackend.persistence.model.AuthMethod
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.persistence.repository.OrganizationDao
import com.ampnet.crowdfundingbackend.persistence.repository.OrganizationFollowerDao
import com.ampnet.crowdfundingbackend.persistence.repository.OrganizationMembershipDao
import com.ampnet.crowdfundingbackend.persistence.repository.RoleDao
import com.ampnet.crowdfundingbackend.persistence.repository.UserDao
import com.ampnet.crowdfundingbackend.service.impl.OrganizationServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@ExtendWith(SpringExtension::class)
@DataJpaTest
@Transactional(propagation = Propagation.SUPPORTS)
@Import(DatabaseCleanerService::class)
class OrganizationServiceTest : TestBase() {

    @Autowired
    private lateinit var databaseCleanerService: DatabaseCleanerService
    @Autowired
    private lateinit var roleDao: RoleDao
    @Autowired
    private lateinit var userDao: UserDao
    @Autowired
    private lateinit var organizationDao: OrganizationDao
    @Autowired
    private lateinit var membershipDao: OrganizationMembershipDao
    @Autowired
    private lateinit var followerDao: OrganizationFollowerDao

    private val organizationService: OrganizationService by lazy {
        OrganizationServiceImpl(organizationDao, membershipDao, followerDao, roleDao, userDao)
    }

    private val user: User by lazy {
        databaseCleanerService.deleteAllUsers()
        createUser("test@email.com", "First", "Last")
    }
    private val organization: Organization by lazy {
        databaseCleanerService.deleteAllOrganizations()
        createOrganization("test org")
    }

    @Test
    fun mustBeAbleToAddUserAsAdminToOrganization() {
        suppose("User exists without any memberships") {
            user.id
            databaseCleanerService.deleteAllOrganizationMemberships()
        }
        suppose("User is added as admin") {
            organizationService.addUserToOrganization(user.id, organization.id, OrganizationRoleType.ORG_ADMIN)
        }

        verify("User has admin role") {
            verifyUserMembership(user.id, organization.id, OrganizationRoleType.ORG_ADMIN)
        }
    }

    @Test
    fun mustBeAbleToAddUserAsMemberToOrganization() {
        suppose("User exists without any memberships") {
            user.id
            databaseCleanerService.deleteAllOrganizationMemberships()
        }
        suppose("User is added to organization as member") {
            organizationService.addUserToOrganization(user.id, organization.id, OrganizationRoleType.ORG_MEMBER)
        }

        verify("User has member role") {
            verifyUserMembership(user.id, organization.id, OrganizationRoleType.ORG_MEMBER)
        }
    }

    @Test
    fun userCanHaveOnlyOneRoleInOrganization() {
        suppose("User exists without any memberships") {
            user.id
            databaseCleanerService.deleteAllOrganizationMemberships()
        }
        suppose("User is added to organization as admin and member") {
            organizationService.addUserToOrganization(user.id, organization.id, OrganizationRoleType.ORG_ADMIN)
            organizationService.addUserToOrganization(user.id, organization.id, OrganizationRoleType.ORG_MEMBER)
        }

        verify("User has only member role") {
            verifyUserMembership(user.id, organization.id, OrganizationRoleType.ORG_MEMBER)
        }
    }

    @Test
    fun userCanFollowOrganization() {
        suppose("User exists without following organizations") {
            user.id
            databaseCleanerService.deleteAllOrganizationFollowers()
        }
        suppose("User started to follow the organization") {
            organizationService.followOrganization(user.id, organization.id)
        }

        verify("User is following the organization") {
            val followers = followerDao.findByOrganizationId(organization.id)
            assertThat(followers).hasSize(1)

            val follower = followers[0]
            assertThat(follower.userId).isEqualTo(user.id)
            assertThat(follower.organizationId).isEqualTo(organization.id)
            assertThat(follower.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
        }
    }

    @Test
    fun userCanUnFollowOrganization() {
        suppose("User is following the organization") {
            databaseCleanerService.deleteAllOrganizationFollowers()
            organizationService.followOrganization(user.id, organization.id)
            val followers = followerDao.findByOrganizationId(organization.id)
            assertThat(followers).hasSize(1)
        }
        suppose("User un followed the organization") {
            organizationService.unfollowOrganization(user.id, organization.id)
        }

        verify("User is not following the organization") {
            val followers = followerDao.findByOrganizationId(organization.id)
            assertThat(followers).hasSize(0)
        }
    }

    private fun verifyUserMembership(userId: Int, organizationId: Int, role: OrganizationRoleType) {
        val memberships = membershipDao.findByUserId(userId)
        assertThat(memberships).hasSize(1)
        val membership = memberships[0]
        assertThat(membership.userId).isEqualTo(userId)
        assertThat(membership.organizationId).isEqualTo(organizationId)
        assertThat(OrganizationRoleType.fromInt(membership.role.id)).isEqualTo(role)
        assertThat(membership.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
    }

    private fun createUser(email: String, firstName: String, lastName: String): User {
        val user = User::class.java.newInstance()
        user.authMethod = AuthMethod.EMAIL
        user.createdAt = ZonedDateTime.now()
        user.email = email
        user.enabled = true
        user.firstName = firstName
        user.lastName = lastName
        user.role = roleDao.getOne(UserRoleType.USER.id)
        return userDao.save(user)
    }

    private fun createOrganization(name: String): Organization {
        val organization = Organization::class.java.newInstance()
        organization.name = name
        organization.legalInfo = "some legal info"
        organization.createdAt = ZonedDateTime.now()
        organization.approved = true
        organization.createdByUser = user
        organization.documents = listOf("hash1", "hash2", "hash3")
        return organizationDao.save(organization)
    }
}
