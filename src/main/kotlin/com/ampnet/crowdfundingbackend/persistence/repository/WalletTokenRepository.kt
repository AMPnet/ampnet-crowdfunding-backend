package com.ampnet.crowdfundingbackend.persistence.repository

import com.ampnet.crowdfundingbackend.persistence.model.WalletToken
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface WalletTokenRepository : JpaRepository<WalletToken, Int> {
    fun findByToken(token: UUID): Optional<WalletToken>
    fun findByUserId(userId: Int): Optional<WalletToken>
}
