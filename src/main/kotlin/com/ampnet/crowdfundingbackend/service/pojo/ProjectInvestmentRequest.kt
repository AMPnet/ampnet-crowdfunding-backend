package com.ampnet.crowdfundingbackend.service.pojo

import com.ampnet.crowdfundingbackend.persistence.model.Project

data class ProjectInvestmentRequest(
    val project: Project,
    val investorUuid: String,
    val amount: Long
)
