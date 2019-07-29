package com.ampnet.crowdfundingbackend.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "com.ampnet.crowdfundingbackend")
class ApplicationProperties {
    var jwt: JwtProperties = JwtProperties()
    val fileStorage: FileStorageProperties = FileStorageProperties()
}

class JwtProperties {
    lateinit var signingKey: String
}

class FileStorageProperties {
    lateinit var url: String
    lateinit var bucket: String
    lateinit var folder: String
}
