package com.ampnet.crowdfundingbackend.service

interface FileStorageService {
    fun saveFile(name: String, content: ByteArray): String
}
