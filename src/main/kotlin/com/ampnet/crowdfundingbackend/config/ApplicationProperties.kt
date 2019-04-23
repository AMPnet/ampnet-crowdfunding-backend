package com.ampnet.crowdfundingbackend.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "com.ampnet.crowdfundingbackend")
class ApplicationProperties {
    var jwt: JwtProperties = JwtProperties()
    val mail: MailProperties = MailProperties()
}

class JwtProperties {
    lateinit var signingKey: String
    var validityInMinutes: Int = 60
}

class MailProperties {
    lateinit var sender: String
    lateinit var confirmationBaseLink: String
    lateinit var organizationInvitationsLink: String
    var enabled: Boolean = false
}

