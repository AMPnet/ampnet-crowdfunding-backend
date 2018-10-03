package com.ampnet.crowdfundingbackend.exception

class InvalidLoginMethodException(exceptionMessage: String) : Exception(exceptionMessage)

class InvalidRequestException(exceptionMessage: String, throwable: Throwable? = null)
    : Exception(exceptionMessage, throwable)

class ResourceAlreadyExistsException(exceptionMessage: String) : Exception(exceptionMessage)

class ResourceNotFoundException(exceptionMessage: String) : Exception(exceptionMessage)

class SocialException(exceptionMessage: String, throwable: Throwable? = null)
    : Exception(exceptionMessage, throwable)
