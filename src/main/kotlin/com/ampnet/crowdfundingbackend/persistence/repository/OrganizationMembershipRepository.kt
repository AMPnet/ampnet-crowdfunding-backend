package com.ampnet.crowdfundingbackend.persistence.repository

import com.ampnet.crowdfundingbackend.persistence.model.OrganizationMembership
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface OrganizationMembershipRepository : JpaRepository<OrganizationMembership, Int> {
    fun findByOrganizationId(organizationId: Int): List<OrganizationMembership>
    fun findByUserUuid(userUuid: UUID): List<OrganizationMembership>
    fun findByOrganizationIdAndUserUuid(organizationId: Int, userUuid: UUID): Optional<OrganizationMembership>
}
