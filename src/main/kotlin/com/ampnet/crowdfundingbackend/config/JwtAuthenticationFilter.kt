package com.ampnet.crowdfundingbackend.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.web.filter.OncePerRequestFilter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class JwtAuthenticationFilter: OncePerRequestFilter() {

    val HEADER_STRING = "JWT-token"
    val TOKEN_PREFIX = ""

    @Autowired
    lateinit var userDetailsService: UserDetailsService

    @Autowired
    lateinit var tokenProvider: TokenProvider

    override fun doFilterInternal(request: HttpServletRequest,
                                  response: HttpServletResponse,
                                  chain: FilterChain) {

        val header = request.getHeader(HEADER_STRING)
        if (header != null && header.startsWith(TOKEN_PREFIX)) {
            val authToken = header.replace(TOKEN_PREFIX, "")
            val username = tokenProvider.getUsernameFromToken(authToken)
            val userDetails = userDetailsService.loadUserByUsername(username)
            if (tokenProvider.validateToken(authToken, userDetails)) {
                val authentication = tokenProvider.getAuthentication(authToken, SecurityContextHolder.getContext().authentication, userDetails)
                authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                SecurityContextHolder.getContext().authentication = authentication
            }
        }

        chain.doFilter(request, response)
    }


}