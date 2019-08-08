package com.ampnet.crowdfundingbackend.service.pojo

import com.ampnet.crowdfunding.proto.TransactionType

enum class PostTransactionType(val type: TransactionType) {
    ORG_CREATE(TransactionType.ORG_CREATE),
    PRJ_CREATE(TransactionType.ORG_ADD_PROJECT),
    PRJ_INVEST(TransactionType.APPROVE),
    PRJ_INVEST_CONFIRM(TransactionType.INVEST),
    ISSUER_MINT(TransactionType.DEPOSIT),
    ISSUER_BURN(TransactionType.WITHDRAW),
    APPROVAL_BURN(TransactionType.APPROVE)
}
