package com.ampnet.crowdfundingbackend.service.pojo

import org.springframework.web.multipart.MultipartFile
import java.util.UUID

data class DocumentSaveRequest(
    val data: ByteArray,
    val name: String,
    val size: Int,
    val type: String,
    val userUuid: UUID
) {
    constructor(file: MultipartFile, userUuid: UUID) : this(
            file.bytes,
            file.originalFilename ?: file.name,
            file.size.toInt(),
            file.contentType ?: file.originalFilename?.split(".")?.lastOrNull() ?: "Unknown",
            userUuid
    )
}
