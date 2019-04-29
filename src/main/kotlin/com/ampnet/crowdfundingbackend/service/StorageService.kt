package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.persistence.model.Document
import com.ampnet.crowdfundingbackend.service.pojo.DocumentSaveRequest

interface StorageService {
    fun saveDocument(request: DocumentSaveRequest): Document
}
