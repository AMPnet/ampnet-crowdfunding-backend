package com.ampnet.crowdfundingbackend.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "com.ampnet.crowdfundingbackend")
class ApplicationProperties {
    var jwt: JwtProperties = JwtProperties()
    val mail: MailProperties = MailProperties()
    val web3j: Web3jProperties = Web3jProperties()
}

class JwtProperties {
    lateinit var signingKey: String
    var validityInMinutes: Int = 60
}

class MailProperties {
    lateinit var sender: String
    lateinit var confirmationBaseLink: String
    var enabled: Boolean = false
}

class Web3jProperties {
    lateinit var clientAddress: String
    var networkId: Int = -1
}
