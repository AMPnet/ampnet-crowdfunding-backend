package com.ampnet.crowdfundingbackend.service.pojo

import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.User

data class ProjectInvestmentRequest(
    val project: Project,
    val investorUuid: String,
    val amount: Long
)
