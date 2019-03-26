package com.ampnet.crowdfundingbackend.enums

enum class TransactionType(val description: String) {
    CREATE_ORG("CreateOrgTx"),
    CREATE_PROJECT("CreateProjectTx"),
    INVEST_ALLOWANCE("InvestAllowanceTx"),
    INVEST("InvestTx")
}
