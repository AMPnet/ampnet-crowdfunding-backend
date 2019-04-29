package com.ampnet.crowdfundingbackend.config

import com.ampnet.crowdfundingbackend.service.CloudStorageService
import org.mockito.Mockito
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Profile("CloudStorageMockConfig")
@Configuration
class CloudStorageMockConfig {

    @Bean
    @Primary
    fun getCloudStorageService(): CloudStorageService {
        return Mockito.mock(CloudStorageService::class.java)
    }
}
