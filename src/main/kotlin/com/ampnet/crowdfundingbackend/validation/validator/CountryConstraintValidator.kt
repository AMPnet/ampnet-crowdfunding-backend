package com.ampnet.crowdfundingbackend.validation.validator

import com.ampnet.crowdfundingbackend.persistence.repository.CountryDao
import com.ampnet.crowdfundingbackend.validation.CountryConstraint
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

class CountryConstraintValidator(val countryDao: CountryDao) : ConstraintValidator<CountryConstraint, Int> {

    override fun initialize(constraintAnnotation: CountryConstraint?) { }

    override fun isValid(countryId: Int?, context: ConstraintValidatorContext): Boolean {
        if (countryId == null) { return true }
        return countryDao.findById(countryId).isPresent
    }
}
