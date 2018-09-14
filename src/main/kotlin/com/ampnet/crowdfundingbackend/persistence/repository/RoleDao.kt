package com.ampnet.crowdfundingbackend.persistence.repository

import com.ampnet.crowdfundingbackend.persistence.model.Role
import org.springframework.data.jpa.repository.JpaRepository

interface RoleDao: JpaRepository<Role, Int> {

}