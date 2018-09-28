package com.ampnet.crowdfundingbackend.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ResourceAlreadyExistsException::class)
    fun handleResourceAlreadyExists(exception: ResourceAlreadyExistsException): ErrorResponse {
        val reason = exception::class.java.canonicalName as String
        return generateErrorResponse(reason, exception.message)
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceDoesNotExists(exception: ResourceNotFoundException): ErrorResponse {
        val reason = exception::class.java.canonicalName as String
        return generateErrorResponse(reason, exception.message)
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidRequestException::class)
    fun handleInvalidRequestException(exception: InvalidRequestException): ErrorResponse {
        val reason = exception::class.java.canonicalName as String
        return generateErrorResponse(reason, exception.message)
    }

    private fun generateErrorResponse(reason: String, message: String?): ErrorResponse {
        // TODO: maybe add data for translation
        val errorMessage = message ?: "Error not defined"
        return ErrorResponse(reason, listOf(errorMessage))
    }
}
