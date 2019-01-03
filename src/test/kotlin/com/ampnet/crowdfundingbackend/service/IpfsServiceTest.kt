package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.TestBase
import com.ampnet.crowdfundingbackend.config.ApplicationProperties
import com.ampnet.crowdfundingbackend.service.impl.IpfsServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension

@Disabled("IPFS testing will be in E2E tests. Missing IPFS host.")
@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [ApplicationProperties::class])
@EnableConfigurationProperties
@Import(IpfsServiceImpl::class)
class IpfsServiceTest : TestBase() {

    @Autowired
    private lateinit var ipfsService: IpfsServiceImpl

    private lateinit var testContext: TestContext

    @BeforeEach
    fun initTestContext() {
        testContext = TestContext()
    }

    @Test
    fun mustLoadApplicationProperties() {
        verify("Service can load application properties") {
            assertThat(ipfsService.ipfs.host).isEqualTo("127.0.0.1")
            assertThat(ipfsService.ipfs.port).isEqualTo(5001)
            assertThat(ipfsService.ipfs.protocol).isEqualTo("http")
        }
    }

    @Test
    fun mustBeAbleToStoreData() {
        verify("Service can store data on IPFS") {
            val testData = "Some text to store".toByteArray()
            val hash = ipfsService.storeData(testData, "Test")
            assertThat(hash).isNotEmpty()
        }
    }

    @Test
    fun mustBeAbleToGetData() {
        suppose("Data is stored on IPFS") {
            testContext.data = "Test data to read".toByteArray()
            testContext.hash = ipfsService.storeData(testContext.data, "Test")
        }

        verify("Service can get data from IPFS") {
            val document = ipfsService.getData(testContext.hash)
            assertThat(document).isEqualTo(testContext.data)
        }
    }

    @Test
    fun mustReturnNullForMissingData() {
        verify("Must handle missing data") {
            val document = ipfsService.getData("QmekUKkE5CTdX6efyQQ2cDNdA8GmXzzWsGjDJ1MgRgDajT")
            assertThat(document).isNull()
        }
    }

    private class TestContext {
        lateinit var data: ByteArray
        lateinit var hash: String
    }
}
