package com.ampnet.crowdfundingbackend.validation.validator

import com.ampnet.crowdfundingbackend.persistence.repository.CountryRepository
import com.ampnet.crowdfundingbackend.validation.CountryConstraint
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

class CountryConstraintValidator(private val countryRepository: CountryRepository)
    : ConstraintValidator<CountryConstraint, Int> {

    override fun initialize(constraintAnnotation: CountryConstraint?) { }

    override fun isValid(countryId: Int?, context: ConstraintValidatorContext): Boolean {
        if (countryId == null) { return true }
        return countryRepository.findById(countryId).isPresent
    }
}
