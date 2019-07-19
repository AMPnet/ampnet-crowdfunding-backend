package com.ampnet.crowdfundingbackend.config

import com.ampnet.crowdfundingbackend.blockchain.BlockchainService
import com.ampnet.crowdfundingbackend.userservice.UserService
import org.mockito.Mockito
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Profile("GrpcServiceMockConfig")
@Configuration
class GrpcServiceMockConfig {

    @Bean
    @Primary
    fun getBlockchainService(): BlockchainService {
        return Mockito.mock(BlockchainService::class.java)
    }

    @Bean
    @Primary
    fun getUserService(): UserService {
        return Mockito.mock(UserService::class.java)
    }
}
