package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.config.ApplicationProperties
import com.ampnet.crowdfundingbackend.exception.IpfsException
import com.ampnet.crowdfundingbackend.service.IpfsService
import org.springframework.stereotype.Service
import io.ipfs.api.IPFS
import io.ipfs.multihash.Multihash
import mu.KLogging
import java.io.IOException
import io.ipfs.api.NamedStreamable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Service
class IpfsServiceImpl(private val applicationProperties: ApplicationProperties) : IpfsService {

    companion object : KLogging()

    val ipfs: IPFS by lazy {
        val it = IPFS(applicationProperties.ipfs.address)
        it.refs.local()
        it
    }

    override fun getData(hash: String): ByteArray? {
        logger.debug { "Searching for document with hash: $hash" }
        val filePointer = Multihash.fromBase58(hash)
        val future = Executors.newCachedThreadPool().submit<ByteArray> { ipfs.cat(filePointer) }
        return try {
            val fileContents = future.get(applicationProperties.ipfs.timeout, TimeUnit.MILLISECONDS)
            logger.debug { "Found document with hash: $hash" }
            fileContents
        } catch (ex: Exception) {
            logger.info(ex) { "Failed to find document with hash: $hash" }
            null
        }
    }

    @Throws(IpfsException::class)
    override fun storeData(data: ByteArray, name: String): String {
        logger.debug { "Storing document: $name" }
        val file = NamedStreamable.ByteArrayWrapper(name, data)
        try {
            val addResult = ipfs.add(file)[0]
            logger.debug { "Successfully stored document: $name \n${addResult.toJSONString()}" }
            return addResult.hash.toBase58()
        } catch (ex: IOException) {
            logger.error(ex) { "Failed to store document: $name" }
            throw IpfsException("Failed to store document: $name", ex)
        }
    }
}
