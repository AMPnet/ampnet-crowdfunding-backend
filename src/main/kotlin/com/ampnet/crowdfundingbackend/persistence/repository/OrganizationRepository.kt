package com.ampnet.crowdfundingbackend.persistence.repository

import com.ampnet.crowdfundingbackend.persistence.model.Organization
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface OrganizationRepository : JpaRepository<Organization, Int> {

    fun findByName(name: String): Optional<Organization>

    // Each Organization in the list will have only one membership because of inner join
    @Query("SELECT org FROM Organization org INNER JOIN FETCH OrganizationMembership mem WHERE mem.userId = ?1")
    fun findAllOrganizationsForUser(userId: Int): List<Organization>
}
