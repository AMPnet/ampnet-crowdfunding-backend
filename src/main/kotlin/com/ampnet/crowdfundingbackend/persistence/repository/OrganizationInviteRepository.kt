package com.ampnet.crowdfundingbackend.persistence.repository

import com.ampnet.crowdfundingbackend.persistence.model.OrganizationInvite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface OrganizationInviteRepository : JpaRepository<OrganizationInvite, Int> {
    fun findByOrganizationIdAndUserId(organizationId: Int, userId: Int): Optional<OrganizationInvite>

    @Query("SELECT invite FROM OrganizationInvite invite " +
            "INNER JOIN FETCH invite.invitedByUser " +
            "INNER JOIN FETCH invite.organization " +
            "WHERE invite.userId = ?1")
    fun findByUserIdWithUserAndOrganizationData(userId: Int): List<OrganizationInvite>
}
