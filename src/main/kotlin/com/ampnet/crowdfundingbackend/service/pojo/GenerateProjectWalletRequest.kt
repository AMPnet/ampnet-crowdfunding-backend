package com.ampnet.crowdfundingbackend.service.pojo

import com.ampnet.crowdfundingbackend.persistence.model.Project

data class GenerateProjectWalletRequest(
    val userWalletHash: String,
    val organizationHash: String,
    val maxPerUser: Long,
    val minPerUser: Long,
    val investmentCap: Long
) {
    constructor(project: Project, organizationHash: String, userWalletHash: String) : this(
            userWalletHash,
            organizationHash,
            project.maxPerUser,
            project.minPerUser,
            project.expectedFunding
    )
}
