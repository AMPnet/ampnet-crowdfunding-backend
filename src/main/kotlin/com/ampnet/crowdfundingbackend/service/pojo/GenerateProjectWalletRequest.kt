package com.ampnet.crowdfundingbackend.service.pojo

import com.ampnet.crowdfundingbackend.persistence.model.Project

data class GenerateProjectWalletRequest(
    val userWallet: String,
    val organization: String,
    val name: String,
    val description: String,
    val maxPerUser: Long,
    val minPerUser: Long,
    val investmentCap: Long
) {
    constructor(project: Project, organizationName: String, userWallet: String): this(
            userWallet,
            organizationName,
            project.name,
            project.description,
            project.maxPerUser,
            project.minPerUser,
            project.expectedFunding
    )
}
