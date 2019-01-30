package com.ampnet.crowdfundingbackend.service.impl

import java.util.Optional

internal object ServiceUtils {
    fun <T> wrapOptional(optional: Optional<T>): T? {
        return if (optional.isPresent) optional.get() else null
    }
}
