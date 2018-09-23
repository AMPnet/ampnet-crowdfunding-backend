package com.ampnet.crowdfundingbackend.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus

@ControllerAdvice
class GlobalExceptionHandler {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ResourceAlreadyExistsException::class)
    @ResponseBody
    fun hangleResourceAlreadyExists(exception: ResourceAlreadyExistsException): ErrorResponse {
        val reason = exception::class.java.canonicalName as String
        return generateErrorResponse(reason, exception.message)
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ResourceNotFoundException::class)
    @ResponseBody
    fun hangleResourceDoesNotExists(exception: ResourceNotFoundException): ErrorResponse {
        val reason = exception::class.java.canonicalName as String
        return generateErrorResponse(reason, exception.message)
    }

    private fun generateErrorResponse(reason: String, message: String?): ErrorResponse {
        // TODO: maybe add data for translation
        val errorMessage = message ?: "Error not defined"
        return ErrorResponse(reason, listOf(errorMessage))
    }
}
