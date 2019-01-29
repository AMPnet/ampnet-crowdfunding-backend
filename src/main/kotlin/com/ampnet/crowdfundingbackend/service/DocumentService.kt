package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.persistence.model.Document
import com.ampnet.crowdfundingbackend.service.pojo.DocumentSaveRequest

interface DocumentService {
    fun saveDocument(request: DocumentSaveRequest): Document
}
