package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.ipfs.IpfsService
import com.ampnet.crowdfundingbackend.persistence.model.Document
import com.ampnet.crowdfundingbackend.persistence.repository.DocumentRepository
import com.ampnet.crowdfundingbackend.service.DocumentService
import com.ampnet.crowdfundingbackend.service.pojo.DocumentSaveRequest
import mu.KLogging
import org.springframework.stereotype.Service
import java.time.ZonedDateTime

@Service
class DocumentServiceImpl(
    private val documentRepository: DocumentRepository,
    private val ipfsService: IpfsService
) : DocumentService {

    companion object : KLogging()

    override fun saveDocument(request: DocumentSaveRequest): Document {
        logger.debug { "Storing document: $request" }

        val ipfsFile = ipfsService.storeData(request.data, request.name)
        logger.debug { "Successfully stored document on IPFS: ${ipfsFile.hash}" }

        val document = Document::class.java.getDeclaredConstructor().newInstance()
        document.hash = ipfsFile.hash
        document.name = request.name
        document.size = request.size
        document.createdAt = ZonedDateTime.now()
        document.createdBy = request.user
        document.type = request.type.take(16)
        return documentRepository.save(document)
    }
}
