package com.ampnet.crowdfundingbackend.validation

import javax.validation.Payload
import javax.validation.ReportAsSingleViolation
import javax.validation.constraints.Size
import kotlin.reflect.KClass

@MustBeDocumented
@ReportAsSingleViolation
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Size(min = 1, max = 30)
annotation class NameConstraint constructor (

    val message: String = "Name must be have at least 1 or at most 30 characters",

    val groups: Array<KClass<*>> = [],

    val payload: Array<KClass<out Payload>> = []
)
