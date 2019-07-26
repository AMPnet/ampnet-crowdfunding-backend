package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.config.ApplicationProperties
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.InternalException
import com.ampnet.crowdfundingbackend.service.CloudStorageService
import mu.KLogging
import org.springframework.stereotype.Service
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.ListObjectsRequest
import software.amazon.awssdk.services.s3.model.ObjectCannedACL
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception
import java.net.URI
import java.time.ZonedDateTime

@Service
class CloudStorageServiceImpl(applicationProperties: ApplicationProperties) : CloudStorageService {

    companion object : KLogging()

    private val endpoint = applicationProperties.fileStorage.url
    private val bucketName = applicationProperties.fileStorage.bucket
    private val folder = applicationProperties.fileStorage.folder
    private val acl = ObjectCannedACL.PUBLIC_READ
    private val digitalOceanSpacesLink = "digitaloceanspaces.com/"

    // Credentials are provided via Env variables: AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY
    private val s3client: S3Client by lazy {
        S3Client.builder()
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .region(Region.EU_CENTRAL_1)
                .endpointOverride(URI(endpoint))
                .build()
    }

    override fun saveFile(name: String, content: ByteArray): String {
        val key = getKeyFromName(name)
        try {
            s3client.putObject(
                    PutObjectRequest.builder().acl(acl).bucket(bucketName).key("$folder/$key").build(),
                    RequestBody.fromBytes(content)
            )
            return getFileLink(key)
        } catch (ex: S3Exception) {
            logger.warn { ex.message }
            throw InternalException(ErrorCode.INT_FILE_STORAGE, "Could not store file with key: $key on cloud\n" +
                    // TODO: remove ex.message after integration-test fix
                    "Exception message: ${ex.message}")
        }
    }

    override fun deleteFile(link: String) {
        val key = getKeyFromLink(link)
        try {
            s3client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(key).build())
        } catch (ex: S3Exception) {
            logger.warn { ex.message }
            throw InternalException(ErrorCode.INT_FILE_STORAGE, "Could not delete file with key: $key on cloud")
        }
    }

    // Only for testing
    fun printObjectsFromBucket() {
        val list = s3client.listObjects(ListObjectsRequest.builder().bucket(bucketName).build())
        println("List name = " + list.name())
        println("List size = " + list.contents().size)
        list.contents().forEach { println(it.key()) }
    }

    fun getFileLink(key: String): String {
        val delimiter = "//"
        val splittedEndpoint = endpoint.split(delimiter)
        return splittedEndpoint[0] + delimiter + bucketName + "." + splittedEndpoint[1] + "/" + folder + "/" + key
    }

    fun getKeyFromName(name: String): String {
        val timestamp = ZonedDateTime.now().toEpochSecond()
        return name.replaceFirst(".", "-$timestamp.")
    }

    fun getKeyFromLink(link: String): String = link.split(digitalOceanSpacesLink)[1]
}
