package com.ampnet.crowdfundingbackend.security

import com.ampnet.crowdfundingbackend.enums.PrivilegeType
import org.springframework.security.test.context.support.WithSecurityContext

@Retention(value = AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
@WithSecurityContext(factory = WithMockUserSecurityFactory::class)
annotation class WithMockCrowdfoundUser(
    val uuid: String = "1234-1234-1234-1234",
    val email: String = "user@email.com",
    val privileges: Array<PrivilegeType> = [],
    val enabled: Boolean = true
)
