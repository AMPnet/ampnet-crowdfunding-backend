package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.persistence.model.Deposit
import com.ampnet.crowdfundingbackend.service.pojo.ApproveDepositRequest
import java.util.UUID

interface DepositService {
    fun create(user: UUID): Deposit
    fun delete(id: Int)
    fun approve(request: ApproveDepositRequest): Deposit
    fun getAllWithDocuments(approved: Boolean): List<Deposit>
    fun findByReference(reference: String): Deposit?
}
