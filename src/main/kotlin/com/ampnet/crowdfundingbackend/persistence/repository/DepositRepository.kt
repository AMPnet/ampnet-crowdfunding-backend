package com.ampnet.crowdfundingbackend.persistence.repository

import com.ampnet.crowdfundingbackend.persistence.model.Deposit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional
import java.util.UUID

interface DepositRepository : JpaRepository<Deposit, Int> {
    @Query("SELECT deposit FROM Deposit deposit LEFT JOIN FETCH deposit.document WHERE deposit.approved = ?1")
    fun findAllWithDocument(approved: Boolean): List<Deposit>

    fun findByReference(reference: String): Optional<Deposit>
    fun findByUserUuid(userUuid: UUID): List<Deposit>
}
