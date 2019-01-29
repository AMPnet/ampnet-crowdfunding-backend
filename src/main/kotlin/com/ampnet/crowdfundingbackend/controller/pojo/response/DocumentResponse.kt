package com.ampnet.crowdfundingbackend.controller.pojo.response

import com.ampnet.crowdfundingbackend.persistence.model.Document
import java.time.ZonedDateTime

data class DocumentResponse(
    val id: Int,
    val hash: String,
    val name: String,
    val type: String,
    val size: Int,
    val createdAt: ZonedDateTime
) {
    constructor(document: Document): this(
            document.id,
            document.hash,
            document.name,
            document.type,
            document.size,
            document.createdAt
    )
}
