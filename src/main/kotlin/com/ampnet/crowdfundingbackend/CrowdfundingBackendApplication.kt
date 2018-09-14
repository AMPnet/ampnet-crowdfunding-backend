package com.ampnet.crowdfundingbackend

import com.ampnet.crowdfundingbackend.persistence.model.User
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties
@EntityScan(
        basePackageClasses = [ User::class ]
)
class CrowdfundingBackendApplication

fun main(args: Array<String>) {
    runApplication<CrowdfundingBackendApplication>(*args)
}
