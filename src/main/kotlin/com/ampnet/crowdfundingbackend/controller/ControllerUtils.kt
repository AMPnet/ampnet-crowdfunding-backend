package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.config.auth.UserPrincipal
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.service.UserService
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.multipart.MultipartFile

internal object ControllerUtils {

    fun getUserFromSecurityContext(userService: UserService): User {
        val userPrincipal = getUserPrincipalFromSecurityContext()
        return userService.find(userPrincipal.email)
                ?: throw ResourceNotFoundException(ErrorCode.USER_MISSING,
                        "Missing user with email: ${userPrincipal.email}")
    }

    fun getUserPrincipalFromSecurityContext(): UserPrincipal =
            SecurityContextHolder.getContext().authentication.principal as UserPrincipal

    fun getFileName(file: MultipartFile): String = file.originalFilename ?: file.name
}
