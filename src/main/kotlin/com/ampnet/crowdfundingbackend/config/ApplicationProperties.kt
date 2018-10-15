package com.ampnet.crowdfundingbackend.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "com.ampnet.crowdfundingbackend")
class ApplicationProperties {
    var jwt: JwtProperties = JwtProperties()
}

class JwtProperties {
    lateinit var signingKey: String
    var validityInMinutes: Int = 60
}
