package com.ampnet.crowdfundingbackend.persistence.repository

import com.ampnet.crowdfundingbackend.persistence.model.PairWalletCode
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface PairWalletCodeRepository : JpaRepository<PairWalletCode, Int> {
    fun findByAddress(address: String): Optional<PairWalletCode>
    fun findByCode(code: String): Optional<PairWalletCode>
}
