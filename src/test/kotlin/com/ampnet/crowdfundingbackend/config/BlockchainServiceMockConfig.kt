package com.ampnet.crowdfundingbackend.config

import com.ampnet.crowdfundingbackend.service.BlockchainService
import org.mockito.Mockito
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Profile("BlockchainServiceMockConfig")
@Configuration
class BlockchainServiceMockConfig {

    @Bean
    @Primary
    fun getBlockchainService(): BlockchainService {
        return Mockito.mock(BlockchainService::class.java)
    }
}
