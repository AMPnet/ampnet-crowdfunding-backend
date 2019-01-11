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

    private val userProfilePath = "/me"
    private val incompleteProfileMessage = "Incomplete user profile"

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication != null && authentication.isAuthenticated && authentication.principal is UserPrincipal) {
            val userPrincipal = authentication.principal as UserPrincipal
            val path = request.requestURI
            if (!userPrincipal.completeProfile && path != userProfilePath) {
                logger.debug("User ${userPrincipal.email} with incomplete profile try to reach $path")
                response.sendError(HttpServletResponse.SC_CONFLICT, incompleteProfileMessage)
                return
            }
        }

        filterChain.doFilter(request, response)
    }
}
