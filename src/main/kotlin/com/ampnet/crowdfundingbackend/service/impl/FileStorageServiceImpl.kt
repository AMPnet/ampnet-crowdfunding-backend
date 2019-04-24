package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.config.ApplicationProperties
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.InternalException
import com.ampnet.crowdfundingbackend.service.FileStorageService
import org.springframework.stereotype.Service
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.ListObjectsRequest
import software.amazon.awssdk.services.s3.model.ObjectCannedACL
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.net.URI
import java.time.ZonedDateTime

@Service
class FileStorageServiceImpl(applicationProperties: ApplicationProperties) : FileStorageService {

    private val endpoint = applicationProperties.fileStorage.url
    private val bucketName = applicationProperties.fileStorage.bucket
    private val folder = applicationProperties.fileStorage.folder
    private val acl = ObjectCannedACL.PUBLIC_READ

    // Credentials are provided via Env variables: AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY
    private val s3client: S3Client by lazy {
        S3Client.builder()
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .region(Region.EU_CENTRAL_1)
                .endpointOverride(URI(endpoint))
                .build()
    }

    override fun saveFile(name: String, content: ByteArray): String {
        val key = "$name-${ZonedDateTime.now().toEpochSecond()}"
        try {
            s3client.putObject(
                    PutObjectRequest.builder().acl(acl).bucket(bucketName).key("$folder/$key").build(),
                    RequestBody.fromBytes(content)
            )
            return getFileLink(key)
        } catch (ex: Exception) {
            throw InternalException(ErrorCode.INT_FILE_STORAGE, "Could not store file with key: $key on cloud")
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
}
