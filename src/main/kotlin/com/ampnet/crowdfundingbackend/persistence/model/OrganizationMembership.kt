package com.ampnet.crowdfundingbackend.persistence.model

import com.ampnet.crowdfundingbackend.enums.OrganizationPrivilegeType
import com.ampnet.crowdfundingbackend.enums.OrganizationRoleType
import java.time.ZonedDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(name = "organization_membership")
data class OrganizationMembership(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int,

    @Column(nullable = false)
    var organizationId: Int,

    @Column(nullable = false)
    var userId: Int,

    @ManyToOne
    @JoinColumn(name = "role_id")
    var role: Role,

    @Column(nullable = false)
    var createdAt: ZonedDateTime
) {
    fun getPrivileges(): List<OrganizationPrivilegeType> {
        return OrganizationRoleType.fromInt(role.id)?.getPrivileges().orEmpty()
    }
}
