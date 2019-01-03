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
            "INNER JOIN FETCH project.organization " +
            "INNER JOIN FETCH project.createdBy " +
            "WHERE project.id = ?1")
    fun findByIdWithOrganizationAndCreator(id: Int): Optional<Project>

    @Query("SELECT project FROM Project project LEFT JOIN FETCH project.wallet WHERE project.id = ?1")
    fun findByIdWithWallet(id: Int): Optional<Project>

//    @Query("SELECT project FROM Project project " +
//            "INNER JOIN FETCH project.documents " +
//            "INNER JOIN FETCH project.investors " +
//            "INNER JOIN FETCH project.createdBy " +
//            "WHERE project.id = ?1")
//    fun findByIdWithAllData(id: Int): Optional<Project>
}
