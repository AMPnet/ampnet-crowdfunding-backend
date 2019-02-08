package com.ampnet.crowdfundingbackend.persistence.repository

import com.ampnet.crowdfundingbackend.persistence.model.Wallet
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface WalletRepository : JpaRepository<Wallet, Int> {
    fun findByHash(hash: String): Optional<Wallet>
}
