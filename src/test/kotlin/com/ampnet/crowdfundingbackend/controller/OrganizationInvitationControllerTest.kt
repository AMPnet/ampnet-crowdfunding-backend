package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.request.OrganizationInviteRequest
import com.ampnet.crowdfundingbackend.controller.pojo.response.OrganizationInvitesListResponse
import com.ampnet.crowdfundingbackend.enums.OrganizationRoleType
import com.ampnet.crowdfundingbackend.enums.PrivilegeType
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.OrganizationInvitation
import com.ampnet.crowdfundingbackend.security.WithMockCrowdfoundUser
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.ZonedDateTime
import java.util.UUID

class OrganizationInvitationControllerTest : ControllerTestBase() {

    private val pathMe = "/invites/me/"
    private val pathOrganization = "/invites/organization/"

    private lateinit var testContext: TestContext

    @BeforeEach
    fun initTestData() {
        testContext = TestContext()
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRO_ORG_INVITE])
    fun mustBeAbleToGetOrganizationInvitations() {
        suppose("User has organization invites") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("Test org", testContext.uuid)
            createOrganizationInvite(defaultEmail, testContext.organization.id, testContext.uuid,
                    OrganizationRoleType.ORG_MEMBER)
        }

        verify("User can get a list of his invites") {
            val result = mockMvc.perform(get(pathMe))
                    .andExpect(status().isOk)
                    .andReturn()

            val invitesResponse: OrganizationInvitesListResponse =
                    objectMapper.readValue(result.response.contentAsString)
            assertThat(invitesResponse.organizationInvites).hasSize(1)
            val invite = invitesResponse.organizationInvites.first()
            assertThat(invite.role).isEqualTo(OrganizationRoleType.ORG_MEMBER)
            assertThat(invite.organizationId).isEqualTo(testContext.organization.id)
            assertThat(invite.organizationName).isEqualTo(testContext.organization.name)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PWO_ORG_INVITE])
    fun mustBeAbleToAcceptOrganizationInvitions() {
        suppose("User has organization invites") {
            databaseCleanerService.deleteAllOrganizations()
            databaseCleanerService.deleteAllOrganizationInvitations()
            testContext.organization = createOrganization("Test org", testContext.uuid)
            createOrganizationInvite(defaultEmail, testContext.organization.id, testContext.uuid,
                    OrganizationRoleType.ORG_MEMBER)
        }

        verify("User can get a list of his invites") {
            mockMvc.perform(post("$pathMe/${testContext.organization.id}/accept"))
                    .andExpect(status().isOk)
        }
        verify("User is a member of organization") {
            val organizations = organizationRepository.findAllOrganizationsForUserUuid(userUuid)
            assertThat(organizations).hasSize(1)
            val organization = organizations.first()
            assertThat(organization.id).isEqualTo(testContext.organization.id)
            val memberships = organization.memberships ?: fail("Organization membership must no be null")
            assertThat(memberships).hasSize(1)
            assertThat(memberships.first().role.id).isEqualTo(OrganizationRoleType.ORG_MEMBER.id)
        }
        verify("User invitation is deleted") {
            val optionalInvite = organizationInviteRepository
                    .findByOrganizationIdAndEmail(testContext.organization.id, defaultEmail)
            assertThat(optionalInvite).isNotPresent
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PWO_ORG_INVITE])
    fun mustBeAbleToRejectOrganizationInvition() {
        suppose("User has organization invites") {
            databaseCleanerService.deleteAllOrganizations()
            databaseCleanerService.deleteAllOrganizationInvitations()
            testContext.organization = createOrganization("Test org", testContext.uuid)
            createOrganizationInvite(defaultEmail, testContext.organization.id, testContext.uuid,
                    OrganizationRoleType.ORG_MEMBER)
        }

        verify("User can get a list of his invites") {
            mockMvc.perform(post("$pathMe/${testContext.organization.id}/reject"))
                    .andExpect(status().isOk)
                    .andReturn()
        }
        verify("User is not a member of organization") {
            val organizations = organizationRepository.findAllOrganizationsForUserUuid(userUuid)
            assertThat(organizations).hasSize(0)
        }
        verify("User invitation is deleted") {
            val optionalInvite = organizationInviteRepository
                    .findByOrganizationIdAndEmail(testContext.organization.id, defaultEmail)
            assertThat(optionalInvite).isNotPresent
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToInviteUserToOrganizationWithOrgAdminRole() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization", userUuid)
        }
        suppose("User has admin role in the organization") {
            addUserToOrganization(userUuid, testContext.organization.id, OrganizationRoleType.ORG_ADMIN)
        }
        suppose("Other user has non organization invites") {
            databaseCleanerService.deleteAllOrganizationInvitations()
        }

        verify("Admin user can invite user to his organization") {
            val request = OrganizationInviteRequest(testContext.invitedEmail, OrganizationRoleType.ORG_MEMBER)
            mockMvc.perform(
                    post("$pathOrganization/${testContext.organization.id}/invite")
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk)
        }
        verify("Organization invite is stored in database") {
            val invites = organizationInviteRepository.findAll()
            assertThat(invites).hasSize(1)
            val invite = invites.first()
            assertThat(invite.email).isEqualTo(testContext.invitedEmail)
            assertThat(invite.organizationId).isEqualTo(testContext.organization.id)
            assertThat(invite.invitedByUserUuid).isEqualTo(userUuid)
            assertThat(invite.role.id).isEqualTo(OrganizationRoleType.ORG_MEMBER.id)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustNotBeAbleToInviteUserToOrganizationWithoutOrgAdminRole() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization", userUuid)
        }
        suppose("User has admin role in the organization") {
            addUserToOrganization(userUuid, testContext.organization.id, OrganizationRoleType.ORG_MEMBER)
        }

        verify("User cannot invite other user without ORG_ADMIN role") {
            val request = OrganizationInviteRequest(testContext.invitedEmail, OrganizationRoleType.ORG_MEMBER)
            mockMvc.perform(
                    post("$pathOrganization/${testContext.organization.id}/invite")
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustNotBeAbleToInviteUserToOrganizationIfNotMemberOfOrganization() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization", testContext.uuid)
        }

        verify("User can invite user to organization if he is not a member of organization") {
            val request = OrganizationInviteRequest("some@user.ocm", OrganizationRoleType.ORG_MEMBER)
            mockMvc.perform(
                    post("$pathOrganization/${testContext.organization.id}/invite")
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToRevokeUserInvitation() {
        suppose("Organization exists") {
            databaseCleanerService.deleteAllOrganizations()
            testContext.organization = createOrganization("test organization", userUuid)
        }
        suppose("User has admin role in the organization") {
            addUserToOrganization(userUuid, testContext.organization.id, OrganizationRoleType.ORG_ADMIN)
        }
        suppose("Other user has organization invites") {
            inviteUserToOrganization(testContext.invitedEmail, testContext.organization.id, userUuid,
                    OrganizationRoleType.ORG_MEMBER)
        }

        verify("User can revoke invitation") {
            mockMvc.perform(
                    post("$pathOrganization/${testContext.organization.id}/${testContext.invitedEmail}/revoke"))
                    .andExpect(status().isOk)
        }
    }

    private fun inviteUserToOrganization(
        email: String,
        organizationId: Int,
        invitedByUuid: UUID,
        role: OrganizationRoleType
    ) {
        val invitation = OrganizationInvitation::class.java.getConstructor().newInstance()
        invitation.email = email
        invitation.organizationId = organizationId
        invitation.invitedByUserUuid = invitedByUuid
        invitation.createdAt = ZonedDateTime.now()
        invitation.role = roleRepository.getOne(role.id)
        organizationInviteRepository.save(invitation)
    }

    private fun createOrganizationInvite(
        email: String,
        organizationId: Int,
        invitedByUuid: UUID,
        role: OrganizationRoleType
    ): OrganizationInvitation {
        val organizationInvite = OrganizationInvitation::class.java.getConstructor().newInstance()
        organizationInvite.email = email
        organizationInvite.organizationId = organizationId
        organizationInvite.invitedByUserUuid = invitedByUuid
        organizationInvite.role = roleRepository.getOne(role.id)
        organizationInvite.createdAt = ZonedDateTime.now()
        return organizationInviteRepository.save(organizationInvite)
    }

    private class TestContext {
        lateinit var organization: Organization
        val uuid: UUID = UUID.randomUUID()
        val invitedEmail = "invited@email.com"
    }
}
