package com.ampnet.crowdfundingbackend.config.auth

import mu.KLogging
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class JwtAuthenticationFilter : OncePerRequestFilter() {

    companion object : KLogging()

    private val headerName = "Authorization"
    private val tokenPrefix = "Bearer "

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val header = request.getHeader(headerName)
        if (header != null && header.startsWith(tokenPrefix)) {
            val authToken = header.replace(tokenPrefix, "")
            SecurityContextHolder.getContext().authentication = JwtAuthToken(authToken)
        }

        chain.doFilter(request, response)
    }
}
