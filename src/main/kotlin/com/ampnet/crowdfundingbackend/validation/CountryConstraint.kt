package com.ampnet.crowdfundingbackend.validation

import com.ampnet.crowdfundingbackend.validation.validator.CountryConstraintValidator
import javax.validation.Constraint
import javax.validation.Payload
import kotlin.reflect.KClass

@MustBeDocumented
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [CountryConstraintValidator::class])
annotation class CountryConstraint constructor (

    val message: String = "Provided country does not exist.",

    val groups: Array<KClass<*>> = [],

    val payload: Array<KClass<out Payload>> = []
)
