package com.ampnet.crowdfundingbackend.enums

enum class PrivilegeType {

    /*
    Def: <type>_privilege

    type:
        - PR - PERM_READ
        - PW - PERM_WRITE
        - PRO - PERM_READ_OWN
        - PWO - PER_WRITE_OWN
        - PRA - PERM_READ_ADMIN
        - PWA - PERM_WRITE_ADMIN
     */

    // Administration
    MONITORING,

    // Profile
    PRO_PROFILE,
    PWO_PROFILE,
    PRA_PROFILE,

    // Organization
    PWA_ORG
}
