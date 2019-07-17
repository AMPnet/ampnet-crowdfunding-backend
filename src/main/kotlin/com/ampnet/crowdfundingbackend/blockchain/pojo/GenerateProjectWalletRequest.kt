package com.ampnet.crowdfundingbackend.blockchain.pojo

import com.ampnet.crowdfundingbackend.persistence.model.Project
import java.time.ZonedDateTime

data class GenerateProjectWalletRequest(
    val userWalletHash: String,
    val organizationHash: String,
    val maxPerUser: Long,
    val minPerUser: Long,
    val investmentCap: Long,
    val endDate: ZonedDateTime
) {
    constructor(project: Project, organizationHash: String, userWalletHash: String) : this(
        userWalletHash,
        organizationHash,
        project.maxPerUser,
        project.minPerUser,
        project.expectedFunding,
        project.endDate
    )
}
