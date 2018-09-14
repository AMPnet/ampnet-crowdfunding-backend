package com.ampnet.crowdfundingbackend.persistence.repository

import com.ampnet.crowdfundingbackend.persistence.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserDao: JpaRepository<User, Long> {

    fun findByUsername(username: String): Optional<User>

}