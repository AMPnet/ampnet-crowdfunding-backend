package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.config.auth.UserPrincipal
import com.ampnet.crowdfundingbackend.exception.TokenException
import org.springframework.security.core.context.SecurityContextHolder

internal object ControllerUtils {
    fun getUserPrincipalFromSecurityContext(): UserPrincipal =
            SecurityContextHolder.getContext().authentication.principal as? UserPrincipal
                    ?: throw TokenException("SecurityContext authentication principal must be UserPrincipal")
}
