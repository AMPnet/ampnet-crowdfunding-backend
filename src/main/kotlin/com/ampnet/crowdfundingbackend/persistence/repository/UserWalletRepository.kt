package com.ampnet.crowdfundingbackend.persistence.repository

import com.ampnet.crowdfundingbackend.persistence.model.UserWallet
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface UserWalletRepository : JpaRepository<UserWallet, Int> {
    fun findByUserUuid(userUuid: String): Optional<UserWallet>
}
