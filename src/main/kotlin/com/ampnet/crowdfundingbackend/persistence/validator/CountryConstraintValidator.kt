package com.ampnet.crowdfundingbackend.persistence.validator

import com.ampnet.crowdfundingbackend.persistence.constraint.ValidCountry
import com.ampnet.crowdfundingbackend.persistence.repository.CountryDao
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

class CountryConstraintValidator(val countryDao: CountryDao): ConstraintValidator<ValidCountry, String> {

    override fun initialize(constraintAnnotation: ValidCountry?) { }

    override fun isValid(countryNicename: String?, context: ConstraintValidatorContext): Boolean {
        if (countryNicename == null) { return true }
        return countryDao.findByNicename(countryNicename).isPresent
    }

}