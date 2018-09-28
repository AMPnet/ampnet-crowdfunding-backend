package com.ampnet.crowdfundingbackend.persistence.validator

import com.ampnet.crowdfundingbackend.persistence.constraint.ValidCountry
import com.ampnet.crowdfundingbackend.persistence.repository.CountryDao
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

class CountryConstraintValidator(val countryDao: CountryDao) : ConstraintValidator<ValidCountry, Int> {

    override fun initialize(constraintAnnotation: ValidCountry?) { }

    override fun isValid(countryId: Int?, context: ConstraintValidatorContext): Boolean {
        if (countryId == null) { return true }
        return countryDao.findById(countryId).isPresent
    }
}
