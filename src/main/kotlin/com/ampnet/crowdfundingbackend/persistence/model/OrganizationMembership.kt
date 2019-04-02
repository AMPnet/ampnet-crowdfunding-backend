package com.ampnet.crowdfundingbackend.persistence.model

import com.ampnet.crowdfundingbackend.enums.OrganizationPrivilegeType
import com.ampnet.crowdfundingbackend.enums.OrganizationRoleType
import java.time.ZonedDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.IdClass
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(name = "organization_membership")
@IdClass(OrganizationUserCompositeId::class)
data class OrganizationMembership(
    @Id
    var organizationId: Int,

    @Id
    var userId: Int,

    @ManyToOne
    @JoinColumn(name = "role_id")
    var role: Role,

    @Column(nullable = false)
    var createdAt: ZonedDateTime
) {
    private fun getPrivileges(): List<OrganizationPrivilegeType> =
            OrganizationRoleType.fromInt(role.id)?.getPrivileges().orEmpty()

    fun hasPrivilegeToSeeOrganizationUsers(): Boolean = getPrivileges().contains(OrganizationPrivilegeType.PR_USERS)

    fun hasPrivilegeToWriteOrganizationUsers(): Boolean = getPrivileges().contains(OrganizationPrivilegeType.PW_USERS)

    fun hasPrivilegeToWriteOrganization(): Boolean = getPrivileges().contains(OrganizationPrivilegeType.PW_ORG)

    fun hasPrivilegeToWriteProject(): Boolean = getPrivileges().contains(OrganizationPrivilegeType.PW_PROJECT)
}
