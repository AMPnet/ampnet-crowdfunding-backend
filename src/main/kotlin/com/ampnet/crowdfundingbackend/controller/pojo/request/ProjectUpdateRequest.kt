package com.ampnet.crowdfundingbackend.controller.pojo.request

data class ProjectUpdateRequest(
    val name: String?,
    val description: String?,
    val location: String?,
    val locationText: String?,
    val returnOnInvestment: String?,
    val active: Boolean?
)
