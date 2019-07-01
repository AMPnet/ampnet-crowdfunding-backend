package com.ampnet.crowdfundingbackend.persistence.repository

import com.ampnet.crowdfundingbackend.persistence.model.OrganizationFollower
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface OrganizationFollowerRepository : JpaRepository<OrganizationFollower, Int> {
    fun findByOrganizationId(organizationId: Int): List<OrganizationFollower>
    fun findByUserUuidAndOrganizationId(userUuid: UUID, organizationId: Int): Optional<OrganizationFollower>
}
