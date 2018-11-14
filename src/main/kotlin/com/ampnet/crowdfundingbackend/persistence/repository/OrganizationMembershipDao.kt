package com.ampnet.crowdfundingbackend.persistence.repository

import com.ampnet.crowdfundingbackend.persistence.model.OrganizationMembership
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OrganizationMembershipDao : JpaRepository<OrganizationMembership, Int> {
    fun findByOrganizationId(organizationId: Int): List<OrganizationMembership>
    fun findByUserId(userId: Int): List<OrganizationMembership>
}
