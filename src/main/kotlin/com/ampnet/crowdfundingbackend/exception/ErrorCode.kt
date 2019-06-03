package com.ampnet.crowdfundingbackend.exception

enum class ErrorCode(val categoryCode: String, var specificCode: String, var message: String) {
    // Registration: 01
    REG_INCOMPLETE("01", "01", "Incomplete signup information"),
    REG_INVALID("01", "02", "Signup information complete but invalid"),
    REG_USER_EXISTS("01", "03", "Signup failed because user exists"),
    REG_EMAIL_INVALID_TOKEN("01", "04", "Failed Email confirmation, invalid token format"),
    REG_EMAIL_NON_EXISTING_TOKEN("01", "05", "Failed Email confirmation, non existing token"),
    REG_EMAIL_EXPIRED_TOKEN("01", "06", "Failed Email confirmation, token expired"),
    REG_SOCIAL("01", "07", "Social exception"),
    REG_IDENTYUM("01", "08", "Identyum exception"),

    // Authentication: 02
    AUTH_INVALID_LOGIN_METHOD("02", "01", "Invalid login method"),

    // Users: 03
    USER_MISSING("03", "01", "Non existing user"),
    USER_ROLE_MISSING("03", "02", "Missing user role"),

    // Wallet: 05
    WALLET_MISSING("05", "01", "Missing wallet"),
    WALLET_EXISTS("05", "02", "Active user cannot create additional wallet"),
    WALLET_FUNDS("05", "03", "User does not have enough funds on wallet"),
    WALLET_HASH_EXISTS("05", "04", "Wallet with this hash already exists"),

    // Organization: 06
    ORG_MISSING("06", "01", "Non existing organization"),
    ORG_PRIVILEGE_APPROVE("06", "02", "Cannot approve organization without privilege"),
    ORG_PRIVILEGE_PW("06", "03",
        "Failed invite user to organization without organization user role, privilege PW_USERS"),
    ORG_DUPLICATE_USER("06", "04", "User is already a member of this organization"),
    ORG_DUPLICATE_INVITE("06", "05", "User is already invited"),
    ORG_DUPLICATE_NAME("06", "06", "Organization with this name already exists"),

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
    PRJ_MAX_FUNDS_PER_USER_TOO_HIGH("07", "10", "Max funding per user is too high"),

    // Internal: 08
    INT_FILE_STORAGE("08", "01", "Could not upload document on cloud file storage"),
    INT_INVALID_VALUE("08", "02", "Invalid value in request"),
    INT_GRPC("08", "50", "Failed gRPC call"),

    // Transaction: 09
    TX_MISSING("09", "01", "Non existing transaction"),
    TX_COMPANION_ID_MISSING("09", "02", "Missing companion id")
}
