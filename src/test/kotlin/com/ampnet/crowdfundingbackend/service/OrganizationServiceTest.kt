package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.config.DatabaseCleanerService
import com.ampnet.crowdfundingbackend.controller.pojo.request.OrganizationInviteRequest
import com.ampnet.crowdfundingbackend.enums.OrganizationRoleType
import com.ampnet.crowdfundingbackend.exception.ResourceAlreadyExistsException
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.service.impl.MailServiceImpl
import com.ampnet.crowdfundingbackend.service.impl.OrganizationServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
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
class OrganizationServiceTest : JpaServiceTestBase() {

    private val mailService: MailServiceImpl = Mockito.mock(MailServiceImpl::class.java)

    private val organizationService: OrganizationService by lazy {
        OrganizationServiceImpl(organizationRepository, membershipRepository, followerRepository, inviteRepository,
                roleRepository, userRepository, mailService)
    }

    private val user: User by lazy {
        databaseCleanerService.deleteAllUsers()
        createUser("test@email.com", "First", "Last")
    }
    private val organization: Organization by lazy {
        databaseCleanerService.deleteAllOrganizations()
        createOrganization("test org", user)
    }

    private lateinit var testContext: TestContext

    @BeforeEach
    fun initTestContext() {
        user.id
        organization.id
        testContext = TestContext()
    }

    @Test
    fun mustBeAbleToAddUserAsAdminToOrganization() {
        suppose("User exists without any memberships") {
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
            databaseCleanerService.deleteAllOrganizationFollowers()
        }
        suppose("User started to follow the organization") {
            organizationService.followOrganization(user.id, organization.id)
        }

        verify("User is following the organization") {
            val followers = followerRepository.findByOrganizationId(organization.id)
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
            val followers = followerRepository.findByOrganizationId(organization.id)
            assertThat(followers).hasSize(1)
        }
        suppose("User un followed the organization") {
            organizationService.unfollowOrganization(user.id, organization.id)
        }

        verify("User is not following the organization") {
            val followers = followerRepository.findByOrganizationId(organization.id)
            assertThat(followers).hasSize(0)
        }
    }

    @Test
    fun adminUserCanInviteOtherUserToOrganization() {
        suppose("User is admin of organization") {
            databaseCleanerService.deleteAllOrganizationMemberships()
            organizationService.addUserToOrganization(user.id, organization.id, OrganizationRoleType.ORG_ADMIN)
        }

        verify("The admin can invite user to organization") {
            testContext.invitedUser = createUser("invited@user.com", "Invited", "User")
            val request = OrganizationInviteRequest(testContext.invitedUser.email, OrganizationRoleType.ORG_MEMBER)
            organizationService.inviteUserToOrganization(request, organization.id, user)
        }
        verify("Invitation is stored in database") {
            val optionalInvitation =
                    inviteRepository.findByOrganizationIdAndUserId(organization.id, testContext.invitedUser.id)
            assertThat(optionalInvitation).isPresent
            val invitation = optionalInvitation.get()
            assertThat(invitation.userId).isEqualTo(testContext.invitedUser.id)
            assertThat(invitation.organizationId).isEqualTo(organization.id)
            assertThat(invitation.invitedBy).isEqualTo(user.id)
            assertThat(OrganizationRoleType.fromInt(invitation.role.id)).isEqualTo(OrganizationRoleType.ORG_MEMBER)
            assertThat(invitation.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
        }
        verify("Sending mail invitation is called") {
            Mockito.verify(mailService, Mockito.times(1))
                    .sendOrganizationInvitationMail(testContext.invitedUser.email, user.getFullName(), organization.name)
        }
    }

    @Test
    fun mustThrowErrorForDuplicateOrganizationInvite() {
        suppose("User has organization invite") {
            databaseCleanerService.deleteAllOrganizationInvites()
            testContext.invitedUser = createUser("invited@user.com", "Invited", "User")
            val request = OrganizationInviteRequest(testContext.invitedUser.email, OrganizationRoleType.ORG_MEMBER)
            organizationService.inviteUserToOrganization(request, organization.id, user)
        }

        verify("Service will throw an error for duplicate user invite to organization") {
            val request = OrganizationInviteRequest(testContext.invitedUser.email, OrganizationRoleType.ORG_MEMBER)
            assertThrows<ResourceAlreadyExistsException> {
                organizationService.inviteUserToOrganization(request, organization.id, user)
            }
        }
    }

    private fun verifyUserMembership(userId: Int, organizationId: Int, role: OrganizationRoleType) {
        val memberships = membershipRepository.findByUserId(userId)
        assertThat(memberships).hasSize(1)
        val membership = memberships[0]
        assertThat(membership.userId).isEqualTo(userId)
        assertThat(membership.organizationId).isEqualTo(organizationId)
        assertThat(OrganizationRoleType.fromInt(membership.role.id)).isEqualTo(role)
        assertThat(membership.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
    }

    private class TestContext {
        lateinit var invitedUser: User
    }
}
