package com.ampnet.crowdfundingbackend.enums

enum class UserRoleType(val id: Int) {

    ADMIN(1) {
        override fun getPrivileges(): List<PrivilegeType> {
            return listOf(
                    PrivilegeType.MONITORING,
                    PrivilegeType.PRA_PROFILE,
                    PrivilegeType.PRO_PROFILE,
                    PrivilegeType.PWO_PROFILE)
        }
    },

    USER(2) {
        override fun getPrivileges(): List<PrivilegeType> {
            return listOf(
                    PrivilegeType.PRO_PROFILE,
                    PrivilegeType.PWO_PROFILE)
        }
    };

    abstract fun getPrivileges(): List<PrivilegeType>
}
