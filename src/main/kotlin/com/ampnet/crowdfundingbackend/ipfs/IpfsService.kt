package com.ampnet.crowdfundingbackend.ipfs

interface IpfsService {
    fun getData(hash: String): ByteArray?
    fun storeData(data: ByteArray, name: String): IpfsFile
}
