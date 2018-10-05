package com.ampnet.crowdfundingbackend.config.auth

import com.ampnet.crowdfundingbackend.exception.TokenException
import mu.KLogging
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class JwtAuthenticationFilter(val tokenProvider: TokenProvider) : OncePerRequestFilter() {

    companion object : KLogging()

    private val headerName = "Authorization"
    private val tokenPrefix = "Bearer "

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {

        val header = request.getHeader(headerName)
        if (header != null && header.startsWith(tokenPrefix)) {
            val authToken = header.replace(tokenPrefix, "")

            try {
                val authentication = tokenProvider.getAuthentication(authToken)
                authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                SecurityContextHolder.getContext().authentication = authentication
            } catch (ex: TokenException) {
                logger.info("Invalid token", ex)
            }
        }

        chain.doFilter(request, response)
    }
}
