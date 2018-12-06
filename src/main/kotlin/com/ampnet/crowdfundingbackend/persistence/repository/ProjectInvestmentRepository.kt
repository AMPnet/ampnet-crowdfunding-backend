package com.ampnet.crowdfundingbackend.persistence.repository

import com.ampnet.crowdfundingbackend.persistence.model.ProjectInvestment
import org.springframework.data.jpa.repository.JpaRepository

interface ProjectInvestmentRepository : JpaRepository<ProjectInvestment, Int> {
    fun findByProjectId(projectId: Int): List<ProjectInvestment>
    fun findByUserId(userId: Int): List<ProjectInvestment>
    fun findByProjectIdAndUserId(projectId: Int, userId: Int): List<ProjectInvestment>
}
