package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.service.DatabaseCleanerService
import org.springframework.stereotype.Service
import javax.persistence.EntityManager
import javax.transaction.Transactional

@Service
class DatabaseCleanerServiceImpl(val em: EntityManager): DatabaseCleanerService {

    @Transactional
    override fun deleteAll() {
        em.createNativeQuery("TRUNCATE app_user CASCADE").executeUpdate()
    }

}
