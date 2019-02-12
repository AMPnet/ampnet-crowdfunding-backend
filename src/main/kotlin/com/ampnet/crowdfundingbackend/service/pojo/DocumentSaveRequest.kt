package com.ampnet.crowdfundingbackend.service.pojo

import com.ampnet.crowdfundingbackend.persistence.model.User
import org.springframework.web.multipart.MultipartFile

data class DocumentSaveRequest(
    val data: ByteArray,
    val name: String,
    val size: Int,
    val type: String,
    val user: User
) {
    constructor(file: MultipartFile, user: User) : this(
            file.bytes,
            file.name,
            file.size.toInt(),
            file.contentType ?: file.originalFilename?.split(".")?.lastOrNull() ?: "Unknown",
            user
    )
}
