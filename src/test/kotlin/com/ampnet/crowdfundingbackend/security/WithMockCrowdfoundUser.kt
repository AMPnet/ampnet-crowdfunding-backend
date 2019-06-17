package com.ampnet.crowdfundingbackend.security

import com.ampnet.crowdfundingbackend.enums.PrivilegeType
import com.ampnet.crowdfundingbackend.enums.UserRoleType
import org.springframework.security.test.context.support.WithSecurityContext

@Retention(value = AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
@WithSecurityContext(factory = WithMockUserSecurityFactory::class)
annotation class WithMockCrowdfoundUser(
    val uuid: String = "1234-1234-1234-1234",
    val email: String = "user@email.com",
    val role: UserRoleType = UserRoleType.USER,
    val privileges: Array<PrivilegeType> = [],
    val enabled: Boolean = true
)
