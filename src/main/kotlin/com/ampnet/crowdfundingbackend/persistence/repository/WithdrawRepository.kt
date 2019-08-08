package com.ampnet.crowdfundingbackend.persistence.repository

import com.ampnet.crowdfundingbackend.persistence.model.Withdraw
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface WithdrawRepository : JpaRepository<Withdraw, Int> {
    @Query("SELECT withdraw FROM Withdraw withdraw " +
            "WHERE withdraw.approvedTxHash IS NOT NULL AND withdraw.burnedTxHash IS NULL")
    fun findAllApproved(): List<Withdraw>

    @Query("SELECT withdraw FROM Withdraw withdraw " +
            "WHERE withdraw.approvedTxHash IS NOT NULL AND withdraw.burnedTxHash IS NOT NULL")
    fun findAllBurned(): List<Withdraw>

    fun findByUserUuid(user: UUID): List<Withdraw>
}
