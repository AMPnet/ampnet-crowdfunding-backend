package com.ampnet.crowdfundingbackend.validation

import javax.validation.Payload
import javax.validation.ReportAsSingleViolation
import javax.validation.constraints.Pattern
import kotlin.reflect.KClass

@MustBeDocumented
@ReportAsSingleViolation
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Pattern(regexp = "(^$|[0-9]{8,12})")
annotation class PhoneNumberConstraint constructor (

    val message: String = "Phone number must consist of 8-12 digits.",

    val groups: Array<KClass<*>> = [],

    val payload: Array<KClass<out Payload>> = []
)
