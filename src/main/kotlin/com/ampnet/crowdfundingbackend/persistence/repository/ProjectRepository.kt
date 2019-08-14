package com.ampnet.crowdfundingbackend.persistence.repository

import com.ampnet.crowdfundingbackend.persistence.model.Project
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface ProjectRepository : JpaRepository<Project, Int> {

    @Query("SELECT project FROM Project project " +
            "INNER JOIN FETCH project.organization " +
            "WHERE project.id = ?1")
    fun findByIdWithOrganization(id: Int): Optional<Project>

    @Query("SELECT project FROM Project project " +
            "LEFT JOIN FETCH project.wallet " +
            "WHERE project.id = ?1")
    fun findByIdWithWallet(id: Int): Optional<Project>

    @Query("SELECT project FROM Project project " +
            "INNER JOIN FETCH project.organization " +
            "LEFT JOIN FETCH project.documents " +
            "LEFT JOIN FETCH project.wallet " +
            "WHERE project.id = ?1")
    fun findByIdWithAllData(id: Int): Optional<Project>

    @Query("SELECT project FROM Project project " +
            "INNER JOIN FETCH project.organization organization " +
            "WHERE organization.id = ?1")
    fun findAllByOrganizationId(organizationId: Int): List<Project>

    @Query("SELECT project FROM Project project " +
            "INNER JOIN FETCH project.wallet " +
            "WHERE project.active=true")
    fun findAllActiveWithWallet(): List<Project>

    fun findByNameContainingIgnoreCase(name: String): List<Project>
}
