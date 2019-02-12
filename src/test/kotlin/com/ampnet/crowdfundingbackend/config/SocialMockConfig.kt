package com.ampnet.crowdfundingbackend.config

import com.ampnet.crowdfundingbackend.service.SocialService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.mockito.Mockito

@Profile("SocialMockConfig")
@Configuration
class SocialMockConfig {

    @Bean
    @Primary
    fun getSocialService(): SocialService {
        return Mockito.mock(SocialService::class.java)
    }
}
