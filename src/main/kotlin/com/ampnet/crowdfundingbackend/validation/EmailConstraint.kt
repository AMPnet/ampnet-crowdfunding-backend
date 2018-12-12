package com.ampnet.crowdfundingbackend.validation

import javax.validation.Constraint
import javax.validation.Payload
import javax.validation.ReportAsSingleViolation
import javax.validation.constraints.Email
import kotlin.reflect.KClass

@MustBeDocumented
@ReportAsSingleViolation
@Constraint(validatedBy = [])
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Email
annotation class EmailConstraint constructor (

    val message: String = "Invalid email format",

    val groups: Array<KClass<*>> = [],

    val payload: Array<KClass<out Payload>> = []
)
