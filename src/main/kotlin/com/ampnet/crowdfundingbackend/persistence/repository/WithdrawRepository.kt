package com.ampnet.crowdfundingbackend.persistence.repository

import com.ampnet.crowdfundingbackend.persistence.model.Withdraw
import org.springframework.data.jpa.repository.JpaRepository

interface WithdrawRepository : JpaRepository<Withdraw, Int> {
    fun findByApproved(approved: Boolean): List<Withdraw>
}
