package com.ampnet.crowdfundingbackend.service.pojo

import com.ampnet.crowdfundingbackend.enums.Currency
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.User
import java.math.BigDecimal

data class ProjectInvestmentRequest(
    val project: Project,
    val investor: User,
    val amount: BigDecimal,
    val currency: Currency
)
