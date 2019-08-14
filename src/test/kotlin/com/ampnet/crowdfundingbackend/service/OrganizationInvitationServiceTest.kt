package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.enums.OrganizationRoleType
import com.ampnet.crowdfundingbackend.exception.ResourceAlreadyExistsException
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.service.impl.OrganizationInviteServiceImpl
import com.ampnet.crowdfundingbackend.service.impl.OrganizationServiceImpl
import com.ampnet.crowdfundingbackend.service.impl.StorageServiceImpl
import com.ampnet.crowdfundingbackend.service.pojo.OrganizationInviteServiceRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.time.ZonedDateTime

class OrganizationInvitationServiceTest : JpaServiceTestBase() {

    private val organizationService: OrganizationService by lazy {
        val storageServiceImpl = StorageServiceImpl(documentRepository, cloudStorageService)
        OrganizationServiceImpl(organizationRepository, membershipRepository,
                roleRepository, mockedBlockchainService, storageServiceImpl)
    }
    private val service: OrganizationInviteService by lazy {
        OrganizationInviteServiceImpl(
                inviteRepository, followerRepository, roleRepository, mailService, organizationService)
    }
    private val organization: Organization by lazy {
        databaseCleanerService.deleteAllOrganizations()
        createOrganization("test org", userUuid)
    }
    private val invitedUser = "invited@email.com"

    @Test
    fun userCanFollowOrganization() {
        suppose("User exists without following organizations") {
            databaseCleanerService.deleteAllOrganizationFollowers()
        }
        suppose("User started to follow the organization") {
            service.followOrganization(userUuid, organization.id)
        }

        verify("User is following the organization") {
            val followers = followerRepository.findByOrganizationId(organization.id)
            assertThat(followers).hasSize(1)

            val follower = followers[0]
            assertThat(follower.userUuid).isEqualTo(userUuid)
            assertThat(follower.organizationId).isEqualTo(organization.id)
            assertThat(follower.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
        }
    }

    @Test
    fun userCanUnFollowOrganization() {
        suppose("User is following the organization") {
            databaseCleanerService.deleteAllOrganizationFollowers()
            service.followOrganization(userUuid, organization.id)
            val followers = followerRepository.findByOrganizationId(organization.id)
            assertThat(followers).hasSize(1)
        }
        suppose("User un followed the organization") {
            service.unfollowOrganization(userUuid, organization.id)
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
            organizationService.addUserToOrganization(userUuid, organization.id, OrganizationRoleType.ORG_ADMIN)
        }

        verify("The admin can invite user to organization") {
            val request = OrganizationInviteServiceRequest(
                    invitedUser, OrganizationRoleType.ORG_MEMBER, organization.id, userUuid)
            service.sendInvitation(request)
        }
        verify("Invitation is stored in database") {
            val optionalInvitation =
                    inviteRepository.findByOrganizationIdAndEmail(organization.id, invitedUser)
            assertThat(optionalInvitation).isPresent
            val invitation = optionalInvitation.get()
            assertThat(invitation.email).isEqualTo(invitedUser)
            assertThat(invitation.organizationId).isEqualTo(organization.id)
            assertThat(invitation.invitedByUserUuid).isEqualTo(userUuid)
            assertThat(OrganizationRoleType.fromInt(invitation.role.id)).isEqualTo(OrganizationRoleType.ORG_MEMBER)
            assertThat(invitation.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
        }
        verify("Sending mail invitation is called") {
            Mockito.verify(mailService, Mockito.times(1))
                    .sendOrganizationInvitationMail(invitedUser, organization.name)
        }
    }

    @Test
    fun mustThrowErrorForDuplicateOrganizationInvite() {
        suppose("User has organization invite") {
            databaseCleanerService.deleteAllOrganizationInvitations()
            val request = OrganizationInviteServiceRequest(
                    invitedUser, OrganizationRoleType.ORG_MEMBER, organization.id, userUuid)
            service.sendInvitation(request)
        }

        verify("Service will throw an error for duplicate user invite to organization") {
            val request = OrganizationInviteServiceRequest(
                    invitedUser, OrganizationRoleType.ORG_MEMBER, organization.id, userUuid)
            assertThrows<ResourceAlreadyExistsException> {
                service.sendInvitation(request)
            }
        }
    }
}
