package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.config.ApplicationProperties
import com.ampnet.crowdfundingbackend.service.impl.CloudStorageServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension

@ActiveProfiles("test")
@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [ApplicationProperties::class])
@EnableConfigurationProperties
class CloudStorageServiceTest {

    @Autowired
    private lateinit var applicationProperties: ApplicationProperties

    private val service: CloudStorageServiceImpl by lazy { CloudStorageServiceImpl(applicationProperties) }

    @Disabled("Not for automated testing")
    @Test
    fun testList() {
        service.printObjectsFromBucket()
    }

    @Disabled("Not for automated testing")
    @Test
    fun testStore() {
        val fileName = "test.txt"
        val fileContent = "Some test data"
        val link = service.saveFile(fileName, fileContent.toByteArray())
        assertThat(link).isNotBlank()
    }

    @Disabled("Not for automated testing")
    @Test
    fun testDelete() {
        service.deleteFile("https://ampnet-storage.ams3.digitaloceanspaces.com/test/test.txt")
    }

    @Test
    fun getKeyForNameTest() {
        val name = "test.txt"
        val key = service.getKeyFromName(name)
        assertThat(key).startsWith("test-").endsWith(".txt")
    }

    @Test
    fun getKeyForNameDoubleDot() {
        val name = "double.dot.txt"
        val key = service.getKeyFromName(name)
        assertThat(key).startsWith("double-").endsWith(".txt")
    }

    @Test
    fun getKeyForName() {
        val name = "test-file.json"
        val key = service.getKeyFromName(name)
        assertThat(key).startsWith("test-file-").endsWith(".json")
    }

    @Test
    fun getLink() {
        val key = "test-32423422.txt"
        val link = service.getFileLink(key)
        val expectedLink = "https://ampnet-storage.ams3.digitaloceanspaces.com/test/$key"
        assertThat(link).isEqualTo(expectedLink)
    }

    @Test
    fun getKeyFromLink() {
        val link = "https://ampnet-storage.ams3.digitaloceanspaces.com/test/test.txt-1556032201"
        val key = service.getKeyFromLink(link)
        assertThat(key).isEqualTo("test/test.txt-1556032201")
    }
}
