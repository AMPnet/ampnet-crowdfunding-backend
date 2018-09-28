package com.ampnet.crowdfundingbackend.persistence.constraint

import com.ampnet.crowdfundingbackend.persistence.validator.CountryConstraintValidator
import javax.validation.Constraint
import javax.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [CountryConstraintValidator::class])
annotation class ValidCountry constructor (

    val message: String = "Invalid Country",

    val groups: Array<KClass<*>> = [],

    val payload: Array<KClass<out Payload>> = []

)