package com.ampnet.crowdfundingbackend.persistence.repository

import com.ampnet.crowdfundingbackend.persistence.model.TransactionInfo
import org.springframework.data.jpa.repository.JpaRepository

interface TransactionInfoRepository : JpaRepository<TransactionInfo, Int>
