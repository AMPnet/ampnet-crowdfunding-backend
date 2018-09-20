package com.ampnet.crowdfundingbackend.security

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.context.support.WithSecurityContextFactory

class WithMockUserSecurityFactory: WithSecurityContextFactory<WithMockCrowdfoundUser> {

    override fun createSecurityContext(annotation: WithMockCrowdfoundUser?): SecurityContext {
        val password = "password"
        val role = SimpleGrantedAuthority("ROLE_" + annotation?.role?.name)

        val token = UsernamePasswordAuthenticationToken(
                annotation?.username,
                password,
                listOf(role)
        )

        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = token
        return context
    }
}
