package com.ampnet.crowdfundingbackend.controller

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object ControllerUtils {

    private val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy - HH:mm:ss Z")

    fun zonedDateTimeToString(zonedDateTime: ZonedDateTime): String {
        return zonedDateTime.format(formatter)
    }
}
