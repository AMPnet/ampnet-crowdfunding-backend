package com.ampnet.crowdfundingbackend.persistence.repository

import com.ampnet.crowdfundingbackend.persistence.model.Document
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface DocumentRepository : JpaRepository<Document, Int> {
    fun findByLink(link: String): Optional<Document>

    @Query("SELECT doc FROM Document doc LEFT JOIN FETCH doc.createdBy WHERE doc.id = ?1")
    fun findByIdWithUser(id: Int): Optional<Document>
}
