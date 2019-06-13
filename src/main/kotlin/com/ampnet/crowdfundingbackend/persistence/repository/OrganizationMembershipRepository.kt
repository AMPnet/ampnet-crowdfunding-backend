package com.ampnet.crowdfundingbackend.persistence.repository

import com.ampnet.crowdfundingbackend.persistence.model.OrganizationMembership
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface OrganizationMembershipRepository : JpaRepository<OrganizationMembership, Int> {
    fun findByOrganizationId(organizationId: Int): List<OrganizationMembership>
    fun findByUserUuid(userUuid: String): List<OrganizationMembership>
    fun findByOrganizationIdAndUserUuid(organizationId: Int, userUuid: String): Optional<OrganizationMembership>
}
