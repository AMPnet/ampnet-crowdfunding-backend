package com.ampnet.crowdfundingbackend.validation

import javax.validation.Constraint
import javax.validation.Payload
import javax.validation.ReportAsSingleViolation
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import kotlin.reflect.KClass

@MustBeDocumented
@ReportAsSingleViolation
@Constraint(validatedBy = [])
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)

// TODO: probably not the best solution
@Min(value = 1)
@Max(value = 1000000000000000)
annotation class InvestmentConstraint(

    val message: String = "Invalid investment number",

    val groups: Array<KClass<*>> = [],

    val payload: Array<KClass<out Payload>> = []

)
