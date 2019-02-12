package com.ampnet.crowdfundingbackend.exception

enum class ErrorCode(val categoryCode: String, val specificCode: String, val message: String) {
    // Registration: 01
    REG_INCOMPLETE("01", "01", "Incomplete signup information"),
    REG_INVALID("01", "02", "Signup information complete but invalid"),
    REG_USER_EXISTS("01", "03", "Signup failed because user exists"),
    REG_EMAIL_INVALID_TOKEN("01", "04", "Failed Email confirmation, invalid token format"),
    REG_EMAIL_NON_EXISTING_TOKEN("01", "05", "Failed Email confirmation, non existing token"),
    REG_EMAIL_EXPIRED_TOKEN("01", "06", "Failed Email confirmation, token expired"),
    REG_SOCIAL("01", "07", "Social exception"),

    // Authentication: 02
    AUTH_MISSING_USER("02", "01", "Non existing user login"),
    AUTH_INVALID_CRED("02", "02", "Invalid credentials"),
    AUTH_INVALID_LOGIN_METHOD("02", "03", "Invalid login method"),

    // Users: 03
    USER_MISSING("03", "01", "Non existing user"),
    USER_NOT_ADMIN("03", "02", "User does not have admin role"),
    USER_INCOMPLETE_PROFILE("03", "03", "User profile is incomplete"),
    USER_OTHER("03", "04", "User cannot access other user profile"),

    // Countries: 04
    COUNTRY_MISSING("04", "01", "No country with specified ID"),

    // Wallet: 05
    WALLET_MISSING("05", "01", "User does not have a wallet"),
    WALLET_EXISTS("05", "02", "Active user cannot create additional wallet"),
    WALLET_FUNDS("05", "03", "User does not have enough funds on wallet"),
    WALLET_TOKEN_MISSING("05", "04", "Missing token"),
    WALLET_TOKEN_EXPIRED("05", "05", "Wallet token has expired"),
    WALLET_HASH_EXISTS("05", "06", "Wallet with this hash already exists"),

    // Organization: 06
    ORG_MISSING("06", "01", "Non existing organization"),
    ORG_PRIVILEGE_APPROVE("06", "02", "Cannot approve organization without privilege"),
    ORG_PRIVILEGE_PW("06", "03",
        "Failed invite user to organization without organization user role, privilege PW_USERS"),
    ORG_DUPLICATE_USER("06", "04", "User is already a member of this organization"),
    ORG_DUPLICATE_INVITE("06", "05", "User is already invited"),
    ORG_DUPLICATE_NAME("06", "06", "Organization with this name alrady exists"),

    // Project: 07
    PRJ_MISSING("07", "01", "Non existing project"),
    PRJ_DATE("07", "02", "Invalid date"),
    PRJ_DATE_EXPIRED("07", "03", "Project has expired"),
    PRJ_MAX_PER_USER("07", "04", "User has exceeded max funds per project"),
    PRJ_MIN_PER_USER("07", "05", "Funding is below project minimum"),
    PRJ_MAX_FUNDS("07", "06", "Project has reached expected funding"),
    PRJ_NOT_ACTIVE("07", "07", "Project is not active"),
    PRJ_MIN_ABOVE_MAX("07", "08",
        "Min investment per user is higher than max investment per user"),
    PRJ_MAX_FUNDS_TOO_HIGH("07", "09", "Expected funding is too high"),
    PRJ_MAX_FUNDS_PER_USER_TOO_HIGH("07", "09", "Max funding per user is too high"),
    PRJ_INVEST_FAILED("07", "10", "Could not invest to project"),

    // Internal: 08
    INT_WALLET_FUNDS("08", "01", "Could not fetch founds from blockchain service."),
    INT_WALLET_ADD("08", "02", "Could not fetch add wallet to blockchain service."),
    INT_TRANSACTION("08", "03", "Could not post transaction on blockchain."),
    INT_ORG("08", "03", "Could not create Organization on blockchain."),
    INT_ORG_ACTIVATE("08", "04", "Could not activate organization on blockchain"),
    INT_IPFS("08", "5", "Could not upload document to IPFS")
}
