package com.ampnet.crowdfundingbackend.service

interface CloudStorageService {
    fun saveFile(name: String, content: ByteArray): String
    fun deleteFile(link: String)
}
