package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.service.pojo.ProjectInvestmentRequest

interface ProjectInvestmentService {
    fun investToProject(request: ProjectInvestmentRequest)
}
