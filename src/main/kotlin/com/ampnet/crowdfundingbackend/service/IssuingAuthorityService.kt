package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.crowdfundingbackend.service.pojo.MintServiceRequest

interface IssuingAuthorityService {
    fun generateMintTransaction(request: MintServiceRequest): TransactionDataAndInfo
    fun confirmMintTransaction(signedTransaction: String): String
}
