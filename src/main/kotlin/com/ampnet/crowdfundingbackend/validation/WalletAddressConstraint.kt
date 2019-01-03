package com.ampnet.crowdfundingbackend.validation

import javax.validation.Constraint
import javax.validation.Payload
import javax.validation.ReportAsSingleViolation
import javax.validation.constraints.Pattern
import kotlin.reflect.KClass

@MustBeDocumented
@ReportAsSingleViolation
@Constraint(validatedBy = [])
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Pattern(regexp = "^0x[a-fA-F0-9]{40}\$")
annotation class WalletAddressConstraint constructor (

    val message: String = "Invalid address",

    val groups: Array<KClass<*>> = [],

    val payload: Array<KClass<out Payload>> = []
)
