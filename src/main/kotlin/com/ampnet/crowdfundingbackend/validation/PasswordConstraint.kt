package com.ampnet.crowdfundingbackend.validation

import com.ampnet.crowdfundingbackend.validation.validator.PasswordConstraintValidator
import javax.validation.Constraint
import javax.validation.Payload
import kotlin.reflect.KClass

@MustBeDocumented
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [PasswordConstraintValidator::class])
annotation class PasswordConstraint constructor (

    val message: String = "Invalid Password",

    val groups: Array<KClass<*>> = [],

    val payload: Array<KClass<out Payload>> = []
)
