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
        logger.warn("ResourceAlreadyExistsException", exception)
        return generateErrorResponse(exception.errorCode, exception.message)
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceDoesNotExists(exception: ResourceNotFoundException): ErrorResponse {
        logger.error("ResourceNotFoundException", exception)
        return generateErrorResponse(exception.errorCode, exception.message)
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidRequestException::class)
    fun handleInvalidRequestException(exception: InvalidRequestException): ErrorResponse {
        logger.warn("InvalidRequestException", exception)
        return generateErrorResponse(exception.errorCode, exception.message)
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(SocialException::class)
    fun handleSocialException(exception: SocialException): ErrorResponse {
        logger.warn("SocialException", exception)
        return generateErrorResponse(exception.errorCode, exception.message)
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(InternalException::class)
    fun handleInternalException(exception: InternalException): ErrorResponse {
        logger.error("InternalException", exception)
        return generateErrorResponse(exception.errorCode, exception.message)
    }

    private fun generateErrorResponse(errorCode: ErrorCode, systemMessage: String?): ErrorResponse {
        val errorMessage = systemMessage ?: "Error not defined"
        val errCode = errorCode.categoryCode + errorCode.specificCode
        return ErrorResponse(errorCode.message, errCode, errorMessage)
    }
}
