package com.ampnet.crowdfundingbackend.enums

enum class UserRoleType(val id: Int) {

    ADMIN(1) {
        override fun getPrivileges(): List<PrivilegeType> {
            return listOf(
                    PrivilegeType.MONITORING,
                    PrivilegeType.PRA_PROFILE,
                    PrivilegeType.PRO_PROFILE,
                    PrivilegeType.PWO_PROFILE,
                    PrivilegeType.PWA_ORG,
                    PrivilegeType.PRO_ORG_INVITE)
        }
    },

    USER(2) {
        override fun getPrivileges(): List<PrivilegeType> {
            return listOf(
                    PrivilegeType.PRO_PROFILE,
                    PrivilegeType.PWO_PROFILE,
                    PrivilegeType.PRO_ORG_INVITE)
        }
    };

    companion object {
        private val map = UserRoleType.values().associateBy(UserRoleType::id)
        fun fromInt(type: Int) = map[type]
    }

    abstract fun getPrivileges(): List<PrivilegeType>
}

enum class OrganizationRoleType(val id: Int) {

    ORG_ADMIN(3) {
        override fun getPrivileges(): List<OrganizationPrivilegeType> {
            return listOf(
                    OrganizationPrivilegeType.PR_USERS,
                    OrganizationPrivilegeType.PW_USERS)
        }
    },

    ORG_MEMBER(4) {
        override fun getPrivileges(): List<OrganizationPrivilegeType> {
            return listOf(
                    OrganizationPrivilegeType.PR_USERS
            )
        }
    };

    companion object {
        private val map = OrganizationRoleType.values().associateBy(OrganizationRoleType::id)
        fun fromInt(type: Int) = map[type]
    }

    abstract fun getPrivileges(): List<OrganizationPrivilegeType>
}
