package com.ampnet.crowdfundingbackend.persistence.repository

import com.ampnet.crowdfundingbackend.persistence.model.MailToken
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface MailTokenDao : JpaRepository<MailToken, Int> {
    fun findByToken(token: String): Optional<MailToken>
    fun findByUserId(userId: Int): Optional<MailToken>
}
