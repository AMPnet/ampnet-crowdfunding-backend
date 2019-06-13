package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.persistence.model.Document
import com.ampnet.crowdfundingbackend.persistence.repository.DocumentRepository
import com.ampnet.crowdfundingbackend.service.StorageService
import com.ampnet.crowdfundingbackend.service.CloudStorageService
import com.ampnet.crowdfundingbackend.service.pojo.DocumentSaveRequest
import mu.KLogging
import org.springframework.stereotype.Service
import java.time.ZonedDateTime

@Service
class StorageServiceImpl(
    private val documentRepository: DocumentRepository,
    private val cloudStorageService: CloudStorageService
) : StorageService {

    companion object : KLogging()

    override fun saveDocument(request: DocumentSaveRequest): Document {
        logger.debug { "Storing document: ${request.name}" }

        val fileLink = storeOnCloud(request.name, request.data)
        logger.debug { "Successfully stored document on cloud: $fileLink" }

        val document = Document::class.java.getDeclaredConstructor().newInstance()
        document.link = fileLink
        document.name = request.name
        document.size = request.size
        document.createdAt = ZonedDateTime.now()
        document.createdByUserUuid = request.userUuid
        document.type = request.type.take(16)
        return documentRepository.save(document)
    }

    override fun saveImage(name: String, content: ByteArray): String {
        logger.debug { "Storing image: $name" }
        val link = storeOnCloud(name, content)
        logger.debug { "Successfully stored image on cloud: $link" }
        return link
    }

    override fun deleteImage(link: String) {
        logger.debug { "Deleting image: $link" }
        cloudStorageService.deleteFile(link)
    }

    private fun storeOnCloud(name: String, content: ByteArray): String = cloudStorageService.saveFile(name, content)
}
