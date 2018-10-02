package com.ampnet.crowdfundingbackend.config

import com.ampnet.crowdfundingbackend.service.UserService
import mu.KLogging
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class JwtAuthenticationFilter(
    val userService: UserService,
    val tokenProvider: TokenProvider
) : OncePerRequestFilter() {

    companion object : KLogging()

    val headerName = "Authorization"
    val tokenPrefix = "Bearer "

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {

        val header = request.getHeader(headerName)
        if (header != null && header.startsWith(tokenPrefix)) {
            val authToken = header.replace(tokenPrefix, "")

            // TODO: try to use inmemory
            val username = tokenProvider.getUsernameFromToken(authToken)
            val userDetails = userService.find(username)
            userDetails.ifPresent { user ->
                if (tokenProvider.validateToken(authToken, user)) {
                    val authentication = tokenProvider.getAuthentication(authToken, user)
                    authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                    SecurityContextHolder.getContext().authentication = authentication
                }
            }
        }

        chain.doFilter(request, response)
    }
}
