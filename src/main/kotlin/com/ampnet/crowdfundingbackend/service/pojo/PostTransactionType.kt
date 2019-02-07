package com.ampnet.crowdfundingbackend.service.pojo

import com.ampnet.crowdfunding.proto.TransactionType

enum class PostTransactionType(val type: TransactionType) {
    ORG_CREATE(TransactionType.ORG_CREATE),
    PRJ_CREATE(TransactionType.ORG_ADD_PROJECT);
}