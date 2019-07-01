package com.ampnet.crowdfundingbackend.security

import com.ampnet.crowdfundingbackend.enums.PrivilegeType
import org.springframework.security.test.context.support.WithSecurityContext

@Retention(value = AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
@WithSecurityContext(factory = WithMockUserSecurityFactory::class)
annotation class WithMockCrowdfoundUser(
    val uuid: String = "89fb3b1c-9c0a-11e9-a2a3-2a2ae2dbcce4",
    val email: String = "user@email.com",
    val privileges: Array<PrivilegeType> = [],
    val enabled: Boolean = true
)
