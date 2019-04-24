package com.ampnet.crowdfundingbackend.config

import com.ampnet.crowdfundingbackend.service.FileStorageService
import org.mockito.Mockito
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Profile("FileStorageMockConfig")
@Configuration
class FileStorageMockConfig {

    @Bean
    @Primary
    fun getFileStorageService(): FileStorageService {
        return Mockito.mock(FileStorageService::class.java)
    }
}
