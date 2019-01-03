package com.ampnet.crowdfundingbackend.persistence.repository

import com.ampnet.crowdfundingbackend.persistence.model.Document
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface DocumentRepository : JpaRepository<Document, Int> {
    fun findByHash(hash: String): Optional<Document>
}
