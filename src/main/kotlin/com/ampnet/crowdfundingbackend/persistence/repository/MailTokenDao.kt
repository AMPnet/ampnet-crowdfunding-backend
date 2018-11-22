package com.ampnet.crowdfundingbackend.persistence.repository

import com.ampnet.crowdfundingbackend.persistence.model.MailToken
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface MailTokenDao : JpaRepository<MailToken, Int> {
    fun findByToken(token: UUID): Optional<MailToken>
    fun findByUserId(userId: Int): Optional<MailToken>
}
