package com.ampnet.crowdfundingbackend.persistence.constraint

import com.ampnet.crowdfundingbackend.persistence.validator.PasswordConstraintValidator
import javax.validation.Constraint
import javax.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [PasswordConstraintValidator::class])
annotation class ValidPassword constructor (

    val message: String = "Invalid Password",

    val groups: Array<KClass<*>> = [],

    val payload: Array<KClass<out Payload>> = []

)