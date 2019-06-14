package com.ampnet.crowdfundingbackend.persistence.repository

import com.ampnet.crowdfundingbackend.persistence.model.OrganizationInvitation
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface OrganizationInviteRepository : JpaRepository<OrganizationInvitation, Int> {
    fun findByOrganizationIdAndEmail(organizationId: Int, email: String): Optional<OrganizationInvitation>
    fun findByEmail(email: String): List<OrganizationInvitation>
}
