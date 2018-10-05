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
        val reason = exception::class.java.canonicalName as String
        return generateErrorResponse(reason, exception.message)
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceDoesNotExists(exception: ResourceNotFoundException): ErrorResponse {
        logger.info("ResourceNotFoundException", exception)
        val reason = exception::class.java.canonicalName as String
        return generateErrorResponse(reason, exception.message)
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidRequestException::class)
    fun handleInvalidRequestException(exception: InvalidRequestException): ErrorResponse {
        logger.info("InvalidRequestException", exception)
        val reason = exception::class.java.canonicalName as String
        return generateErrorResponse(reason, exception.message)
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(SocialException::class)
    fun handleSocialException(exception: SocialException): ErrorResponse {
        logger.info("SocialException", exception)
        val reason = exception::class.java.canonicalName as String
        return generateErrorResponse(reason, exception.message)
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidLoginMethodException::class)
    fun handleInvalidLoginMethod(exception: InvalidLoginMethodException): ErrorResponse {
        logger.info("InvalidRequestException", exception)
        val reason = exception::class.java.canonicalName as String
        return generateErrorResponse(reason, exception.message)
    }

    private fun generateErrorResponse(reason: String, message: String?): ErrorResponse {
        // TODO: maybe add data for translation
        val errorMessage = message ?: "Error not defined"
        return ErrorResponse(reason, listOf(errorMessage))
    }
}
