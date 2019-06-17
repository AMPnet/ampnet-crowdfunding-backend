package com.ampnet.crowdfundingbackend.config.auth

import mu.KLogging
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class ProfileFilter : OncePerRequestFilter() {

    companion object : KLogging()

    private val disabledProfileMessage = "Disabled user profile"

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication != null && authentication.isAuthenticated) {
            val principal = authentication.principal
            if (principal is UserPrincipal) {
                val path = request.requestURI
                if (principal.enabled.not()) {
                    logger.warn("User: ${principal.uuid} with disabled profile try to reach $path")
                    response.sendError(HttpServletResponse.SC_CONFLICT, disabledProfileMessage)
                    return
                }
            }
        }

        filterChain.doFilter(request, response)
    }
}
