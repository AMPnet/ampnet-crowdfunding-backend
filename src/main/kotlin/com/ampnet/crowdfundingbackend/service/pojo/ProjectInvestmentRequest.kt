package com.ampnet.crowdfundingbackend.service.pojo

import com.ampnet.crowdfundingbackend.persistence.model.Project
import java.util.UUID

data class ProjectInvestmentRequest(
    val project: Project,
    val investorUuid: UUID,
    val amount: Long
)
