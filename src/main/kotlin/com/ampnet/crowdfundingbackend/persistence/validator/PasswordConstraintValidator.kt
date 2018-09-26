package com.ampnet.crowdfundingbackend.persistence.validator

import com.ampnet.crowdfundingbackend.persistence.constraint.ValidPassword
import org.passay.*
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

class PasswordConstraintValidator: ConstraintValidator<ValidPassword, String> {

    override fun initialize(constraintAnnotation: ValidPassword?) { }

    override fun isValid(password: String?, context: ConstraintValidatorContext): Boolean {

        if (password == null) { return true }

        val validator = PasswordValidator(listOf(
                LengthRule(8,30),
                CharacterRule(EnglishCharacterData.UpperCase, 1),
                CharacterRule(EnglishCharacterData.Digit, 1),
                CharacterRule(EnglishCharacterData.Special, 1),
                IllegalSequenceRule(EnglishSequenceData.Numerical, 3, false),
                IllegalSequenceRule(EnglishSequenceData.Alphabetical, 3, false),
                IllegalSequenceRule(EnglishSequenceData.USQwerty, 3, false),
                WhitespaceRule()
        ))

        val result = validator.validate(PasswordData(password))
        if (result.isValid)  { return true }

        context.disableDefaultConstraintViolation()
        context.buildConstraintViolationWithTemplate(
                validator.getMessages(result).joinToString(" ")
        ).addConstraintViolation()

        return false

    }

}