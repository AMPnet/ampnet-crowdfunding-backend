package com.ampnet.crowdfundingbackend.ipfs

import com.ampnet.crowdfundingbackend.config.ApplicationProperties
import com.ampnet.crowdfundingbackend.exception.IpfsException
import org.springframework.stereotype.Service
import io.ipfs.api.IPFS
import io.ipfs.api.MerkleNode
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
    override fun storeData(data: ByteArray, name: String): IpfsFile {
        logger.debug { "Storing document: $name" }
        val file = NamedStreamable.ByteArrayWrapper(name, data)
        try {
            val addResult = ipfs.add(file)[0]
            logger.debug { "Successfully stored document: $name \n${addResult.toJSONString()}" }
            val size = getSize(addResult)
            val nodeName = getName(addResult) ?: name
            val hash = addResult.hash.toBase58()
            return IpfsFile(hash, nodeName, size)
        } catch (ex: IOException) {
            logger.error(ex) { "Failed to store document: $name" }
            throw IpfsException("Failed to store document: $name", ex)
        }
    }

    private fun getSize(merkleNode: MerkleNode): Int? {
        if (merkleNode.size.isPresent) {
            return merkleNode.size.get()
        }
        if (merkleNode.largeSize.isPresent) {
            return merkleNode.largeSize.get().toIntOrNull()
        }
        return null
    }

    private fun getName(merkleNode: MerkleNode): String? {
        if (merkleNode.name.isPresent) {
            return merkleNode.name.get()
        }
        return null
    }
}
