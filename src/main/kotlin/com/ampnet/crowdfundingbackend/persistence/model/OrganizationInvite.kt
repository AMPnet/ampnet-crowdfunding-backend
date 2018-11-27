package com.ampnet.crowdfundingbackend.persistence.model

import java.time.ZonedDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.IdClass
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(name = "organization_invite")
@IdClass(OrganizationUserCompositeId::class)
data class OrganizationInvite(

    @Id
    var organizationId: Int,

    @Id
    var userId: Int,

    @Column(nullable = false)
    var invitedBy: Int,

    @ManyToOne
    @JoinColumn(name = "role_id")
    var role: Role,

    @Column(nullable = false)
    var createdAt: ZonedDateTime
)
