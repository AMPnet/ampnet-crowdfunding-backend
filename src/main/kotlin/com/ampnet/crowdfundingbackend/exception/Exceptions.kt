package com.ampnet.crowdfundingbackend.exception

class InvalidRequestException(val errorCode: ErrorCode, exceptionMessage: String, throwable: Throwable? = null) :
    Exception(exceptionMessage, throwable)

class ResourceAlreadyExistsException(val errorCode: ErrorCode, exceptionMessage: String) : Exception(exceptionMessage)

class ResourceNotFoundException(val errorCode: ErrorCode, exceptionMessage: String) : Exception(exceptionMessage)

class SocialException(val errorCode: ErrorCode, exceptionMessage: String, throwable: Throwable? = null) :
    Exception(exceptionMessage, throwable)

class TokenException(exceptionMessage: String, throwable: Throwable? = null) : Exception(exceptionMessage, throwable)

class InternalException(val errorCode: ErrorCode, exceptionMessage: String, throwable: Throwable? = null) :
    Exception(exceptionMessage, throwable)
