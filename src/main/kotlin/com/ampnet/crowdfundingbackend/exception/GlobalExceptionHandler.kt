package com.ampnet.crowdfundingbackend.exception

import mu.KLogging
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    companion object : KLogging()

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ResourceAlreadyExistsException::class)
    fun handleResourceAlreadyExists(exception: ResourceAlreadyExistsException): ErrorResponse {
        logger.info("ResourceAlreadyExistsException", exception)
        return generateErrorResponse(exception.errorCode, exception.message)
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceDoesNotExists(exception: ResourceNotFoundException): ErrorResponse {
        logger.info("ResourceNotFoundException", exception)
        return generateErrorResponse(exception.errorCode, exception.message)
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidRequestException::class)
    fun handleInvalidRequestException(exception: InvalidRequestException): ErrorResponse {
        logger.info("InvalidRequestException", exception)
        return generateErrorResponse(exception.errorCode, exception.message)
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(SocialException::class)
    fun handleSocialException(exception: SocialException): ErrorResponse {
        logger.info("SocialException", exception)
        return generateErrorResponse(exception.errorCode, exception.message)
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidLoginMethodException::class)
    fun handleInvalidLoginMethod(exception: InvalidLoginMethodException): ErrorResponse {
        logger.info("InvalidRequestException", exception)
        return generateErrorResponse(ErrorCode.AUTH_INVALID_LOGIN_METHOD, exception.message)
    }

    private fun generateErrorResponse(errorCode: ErrorCode, systemMessage: String?): ErrorResponse {
        val errorMessage = systemMessage ?: "Error not defined"
        val errCode = errorCode.categoryCode + errorCode.specificCode
        return ErrorResponse(errorCode.message, errCode, errorMessage)
    }
}
