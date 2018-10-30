package com.ampnet.crowdfundingbackend.config

import org.springframework.stereotype.Service
import javax.persistence.EntityManager
import javax.transaction.Transactional

@Service
class DatabaseCleanerService(val em: EntityManager) {

    @Transactional
    fun deleteAll() {
        em.createNativeQuery("TRUNCATE app_user CASCADE").executeUpdate()
    }
}
