package com.ampnet.crowdfundingbackend.persistence.repository

import com.ampnet.crowdfundingbackend.persistence.model.OrganizationMembership
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface OrganizationMembershipRepository : JpaRepository<OrganizationMembership, Int> {
    fun findByOrganizationId(organizationId: Int): List<OrganizationMembership>
    fun findByUserId(userId: Int): List<OrganizationMembership>
    fun findByOrganizationIdAndUserId(organizationId: Int, userId: Int): Optional<OrganizationMembership>
}
