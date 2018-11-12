package com.ampnet.crowdfundingbackend.persistence

import javax.persistence.AttributeConverter
import javax.persistence.Converter

@Converter
class HashArrayToStringConverter : AttributeConverter<List<String>, String> {

    private val separator = ";"

    override fun convertToDatabaseColumn(attribute: List<String>?): String? {
        return attribute?.joinToString(separator = separator)
    }

    override fun convertToEntityAttribute(dbData: String?): List<String> {
        if (dbData.isNullOrBlank()) {
            return emptyList()
        }
        return dbData.split(separator)
    }
}
