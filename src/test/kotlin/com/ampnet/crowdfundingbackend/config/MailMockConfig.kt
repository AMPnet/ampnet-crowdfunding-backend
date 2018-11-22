package com.ampnet.crowdfundingbackend.config

import com.ampnet.crowdfundingbackend.service.MailService
import org.mockito.Mockito
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Profile("MailMockConfig")
@Configuration
class MailMockConfig {

    @Bean
    @Primary
    fun getMailService(): MailService {
        return Mockito.mock(MailService::class.java)
    }
}
