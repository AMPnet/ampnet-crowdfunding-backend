package com.ampnet.crowdfundingbackend.service

interface IpfsService {
    fun getData(hash: String): ByteArray?
    fun storeData(data: ByteArray, name: String): String
}
