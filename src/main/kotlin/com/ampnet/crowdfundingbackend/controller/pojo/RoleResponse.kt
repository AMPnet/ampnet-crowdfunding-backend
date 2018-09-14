package com.ampnet.crowdfundingbackend.controller.pojo

import com.ampnet.crowdfundingbackend.persistence.model.Role

data class RoleResponse(val name: String) {
    constructor(role: Role) : this (role.name)
}