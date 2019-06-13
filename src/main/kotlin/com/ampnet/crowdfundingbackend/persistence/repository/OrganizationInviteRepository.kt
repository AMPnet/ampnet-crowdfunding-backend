package com.ampnet.crowdfundingbackend.persistence.repository

import com.ampnet.crowdfundingbackend.persistence.model.OrganizationInvite
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface OrganizationInviteRepository : JpaRepository<OrganizationInvite, Int> {
    fun findByOrganizationIdAndEmail(organizationId: Int, email: String): Optional<OrganizationInvite>
    fun findByEmail(email: String): List<OrganizationInvite>
}
