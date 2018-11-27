package com.ampnet.crowdfundingbackend.persistence.repository

import com.ampnet.crowdfundingbackend.persistence.model.OrganizationInvite
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface OrganizationInviteRepository : JpaRepository<OrganizationInvite, Int> {
    fun findByOrganizationIdAndUserId(organizationId: Int, userId: Int): Optional<OrganizationInvite>
}
