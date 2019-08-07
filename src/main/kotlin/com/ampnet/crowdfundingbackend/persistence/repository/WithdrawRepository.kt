package com.ampnet.crowdfundingbackend.persistence.repository

import com.ampnet.crowdfundingbackend.persistence.model.Withdraw
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface WithdrawRepository : JpaRepository<Withdraw, Int> {
    fun findByUser(user: UUID): List<Withdraw>
}
