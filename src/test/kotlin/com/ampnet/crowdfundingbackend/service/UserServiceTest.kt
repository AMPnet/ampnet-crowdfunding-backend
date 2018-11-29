package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.enums.OrganizationRoleType
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.service.impl.UserServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.ZonedDateTime

@Disabled
class UserServiceTest : JpaServiceTestBase() {

    private val userService: UserService by lazy {
        val mailService = Mockito.mock(MailService::class.java)
        UserServiceImpl(userRepository, roleRepository, countryRepository, mailRepository,
                mailService, passwordEncoder)
    }

    private val admin: User by lazy {
        databaseCleanerService.deleteAllUsers()
        createUser("admin@test.com", "Admin", "User")
    }
    private val organization: Organization by lazy {
        databaseCleanerService.deleteAllOrganizations()
        createOrganization("User Service Organization", admin)
    }
    private val user: User by lazy {
        admin.id
        createUser("user@test.com", "Invited", "User")
    }

    @Test
    fun mustReturnUserOrganizationInvite() {
        suppose("User has organization invites") {
            databaseCleanerService.deleteAllOrganizationInvites()
            createOrganizationInvite(user.id, organization.id, OrganizationRoleType.ORG_MEMBER, admin.id)
        }

        verify("User service can return list of invites with the organization and user data") {
            val invites = inviteRepository.findByUserIdWithUserAndOrganizationData(user.id)
            assertThat(invites).hasSize(1)
            val invite = invites.first()
            assertThat(invite.userId).isEqualTo(user.id)
            assertThat(invite.organizationId).isEqualTo(organization.id)
            assertThat(invite.invitedBy).isEqualTo(admin.id)
            assertThat(invite.role.id).isEqualTo(OrganizationRoleType.ORG_MEMBER.id)
            assertThat(invite.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(invite.organization).isNotNull
            assertThat(invite.organization!!.id).isEqualTo(organization.id)
            assertThat(invite.invitedByUser).isNotNull
            assertThat(invite.invitedByUser!!.id).isEqualTo(admin.id)
        }
    }

    @Test
    fun mustReturnOnlyUserSpecificOrganizationInvites() {
        suppose("User has multiple invitations") {
            databaseCleanerService.deleteAllOrganizationInvites()
            val organization2 = createOrganization("Org 2", admin)
            val organization3 = createOrganization("Org 3", admin)
            createOrganizationInvite(user.id, organization2.id, OrganizationRoleType.ORG_ADMIN, admin.id)
            createOrganizationInvite(user.id, organization3.id, OrganizationRoleType.ORG_MEMBER, admin.id)
            createOrganizationInvite(admin.id, organization2.id, OrganizationRoleType.ORG_MEMBER, admin.id)
            createOrganizationInvite(admin.id, organization3.id, OrganizationRoleType.ORG_MEMBER, admin.id)
        }

        verify("The service returns only user specific invites") {
            val invites = inviteRepository.findByUserIdWithUserAndOrganizationData(user.id)
            assertThat(invites).hasSize(2)
            assertThat(invites.map { it.organizationId }).doesNotContain(organization.id)
            assertThat(invites[0].organization).isNotNull
            assertThat(invites[0].invitedByUser).isNotNull
            assertThat(invites[1].organization).isNotNull
            assertThat(invites[1].invitedByUser).isNotNull
        }
    }
}
