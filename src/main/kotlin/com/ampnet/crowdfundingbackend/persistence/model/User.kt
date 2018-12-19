package com.ampnet.crowdfundingbackend.persistence.model

import com.ampnet.crowdfundingbackend.enums.AuthMethod
import com.ampnet.crowdfundingbackend.enums.UserRoleType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.ZonedDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.Table

@Entity
@Table(name = "app_user")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int,

    @Column(nullable = false)
    var email: String,

    @Column
    var password: String?,

    @Column
    var firstName: String?,

    @Column
    var lastName: String?,

    @ManyToOne
    @JoinColumn(name = "country_id")
    var country: Country?,

    @Column
    var phoneNumber: String?,

    @ManyToOne
    @JoinColumn(name = "role_id")
    var role: Role,

    @Column(nullable = false)
    var createdAt: ZonedDateTime,

    @Enumerated(EnumType.STRING)
    @Column(length = 8)
    var authMethod: AuthMethod,

    @Column(nullable = false)
    var enabled: Boolean,

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId")
    var organizations: List<OrganizationMembership>?,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id")
    var wallet: Wallet?

) {
    fun getAuthorities(): Set<SimpleGrantedAuthority> {
        val roleAuthority = SimpleGrantedAuthority("ROLE_" + role.name)
        val privileges = UserRoleType.fromInt(role.id)
                ?.getPrivileges()
                ?.map { SimpleGrantedAuthority(it.name) }.orEmpty()
        return (privileges + roleAuthority).toSet()
    }

    fun getFullName(): String = "$firstName $lastName"
}
