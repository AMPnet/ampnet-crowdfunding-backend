package com.ampnet.crowdfundingbackend.persistence.repository

import com.ampnet.crowdfundingbackend.persistence.model.OrganizationFollower
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface OrganizationFollowerRepository : JpaRepository<OrganizationFollower, Int> {
    fun findByOrganizationId(organizationId: Int): List<OrganizationFollower>
    fun findByUserIdAndOrganizationId(userId: Int, organizationId: Int): Optional<OrganizationFollower>
}
