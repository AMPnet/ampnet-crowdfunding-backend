package com.ampnet.crowdfundingbackend.security

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.context.support.WithSecurityContextFactory

class WithMockUserSecurityFactory : WithSecurityContextFactory<WithMockCrowdfoundUser> {

    private val password = "password"

    override fun createSecurityContext(annotation: WithMockCrowdfoundUser?): SecurityContext {
        val authorities = mapPrivilegesOrRoleToAuhtorities(annotation)
        val token = UsernamePasswordAuthenticationToken(
                annotation?.username,
                password,
                authorities
        )

        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = token
        return context
    }

    private fun mapPrivilegesOrRoleToAuhtorities(annotation: WithMockCrowdfoundUser?): List<SimpleGrantedAuthority> {
        return annotation?.privileges?.map { SimpleGrantedAuthority(it.name) }
                ?: annotation?.role?.getPrivileges()?.map { SimpleGrantedAuthority(it.name) }
                        .orEmpty()
    }
}
