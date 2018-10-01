package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.service.DatabaseCleanerService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.persistence.EntityManager
import javax.transaction.Transactional

@Service
class DatabaseCleanerServiceImpl: DatabaseCleanerService {

    @Autowired
    private lateinit var em: EntityManager

    @Transactional
    override fun deleteAll() {
        em.createNativeQuery("TRUNCATE app_user CASCADE").executeUpdate()
    }

}
