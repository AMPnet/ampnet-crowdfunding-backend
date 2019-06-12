package com.ampnet.crowdfundingbackend.config.auth

import mu.KLogging
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

// TODO: think about removing this filter
@Component
class ProfileFilter : OncePerRequestFilter() {

    companion object : KLogging()

    private val userProfilePath = "/me"
    private val incompleteProfileMessage = "Incomplete user profile"

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
                if (!principal.completeProfile && path != userProfilePath) {
                    logger.debug("User ${principal.email} with incomplete profile try to reach $path")
                    response.sendError(HttpServletResponse.SC_CONFLICT, incompleteProfileMessage)
                    return
                }
            }
        }

        filterChain.doFilter(request, response)
    }
}
