package com.ampnet.crowdfundingbackend.persistence.repository

import com.ampnet.crowdfundingbackend.persistence.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface UserRepository : JpaRepository<User, Int> {
    fun findByEmail(email: String): Optional<User>

    // Each User in the list will have only one membership because of inner join
    @Query("SELECT u FROM User u INNER JOIN FETCH u.organizations mem WHERE mem.organizationId = ?1")
    fun findAllUserForOrganization(organizationId: Int): List<User>
}
