package com.ampnet.crowdfundingbackend.persistence.model

import com.ampnet.crowdfundingbackend.enums.OrganizationPrivilegeType
import com.ampnet.crowdfundingbackend.enums.OrganizationRoleType
import java.io.Serializable
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
@IdClass(OrganizationMembershipId::class)
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
    fun getPrivileges(): List<OrganizationPrivilegeType> {
        return OrganizationRoleType.fromInt(role.id)?.getPrivileges().orEmpty()
    }
}

class OrganizationMembershipId() : Serializable {

    protected var organizationId: Int = -1
    protected var userId: Int = -1

    constructor(organizationId: Int, userId: Int): this() {
        this.organizationId = organizationId
        this.userId = userId
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OrganizationMembershipId

        if (organizationId != other.organizationId) return false
        if (userId != other.userId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = organizationId
        result = 31 * result + userId
        return result
    }
}
