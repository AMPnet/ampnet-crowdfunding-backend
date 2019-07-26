package com.ampnet.crowdfundingbackend.persistence.repository

import com.ampnet.crowdfundingbackend.persistence.model.Organization
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional
import java.util.UUID

interface OrganizationRepository : JpaRepository<Organization, Int> {

    @Query("SELECT org FROM Organization org LEFT JOIN FETCH org.documents WHERE org.id = ?1")
    fun findByIdWithDocuments(organizationId: Int): Optional<Organization>

    // Each Organization in the list will have only one membership because of inner join
    @Query("SELECT org FROM Organization org INNER JOIN FETCH org.memberships mem WHERE mem.userUuid = ?1")
    fun findAllOrganizationsForUserUuid(userUuid: UUID): List<Organization>

    fun findByName(name: String): Optional<Organization>

    fun findByNameContainingIgnoreCase(name: String): List<Organization>
}
