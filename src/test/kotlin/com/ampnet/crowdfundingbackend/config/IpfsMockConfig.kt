package com.ampnet.crowdfundingbackend.config

import com.ampnet.crowdfundingbackend.ipfs.IpfsService
import org.mockito.Mockito
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Profile("IpfsMockConfig")
@Configuration
class IpfsMockConfig {

    @Bean
    @Primary
    fun getIpfsService(): IpfsService {
        return Mockito.mock(IpfsService::class.java)
    }
}
