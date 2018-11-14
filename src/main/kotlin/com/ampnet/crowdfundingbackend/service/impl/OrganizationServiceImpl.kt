package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.enums.OrganizationRoleType
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.OrganizationMembership
import com.ampnet.crowdfundingbackend.persistence.model.Role
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.persistence.repository.OrganizationDao
import com.ampnet.crowdfundingbackend.persistence.repository.OrganizationMembershipDao
import com.ampnet.crowdfundingbackend.persistence.repository.RoleDao
import com.ampnet.crowdfundingbackend.persistence.repository.UserDao
import com.ampnet.crowdfundingbackend.service.OrganizationService
import com.ampnet.crowdfundingbackend.service.pojo.OrganizationServiceRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Service
class OrganizationServiceImpl(
    private val organizationDao: OrganizationDao,
    private val membershipDao: OrganizationMembershipDao,
    private val roleDao: RoleDao,
    private val userDao: UserDao
) : OrganizationService {

    private val adminRole: Role by lazy { roleDao.getOne(OrganizationRoleType.ADMIN.id) }
    private val memberRole: Role by lazy { roleDao.getOne(OrganizationRoleType.MEMBER.id) }

    @Transactional
    override fun createOrganization(serviceRequest: OrganizationServiceRequest): Organization {
        val organization = Organization::class.java.newInstance()
        organization.name = serviceRequest.name
        organization.createdByUser = serviceRequest.owner
        organization.legalInfo = serviceRequest.legalInfo
        organization.documents = serviceRequest.documentHashes
        organization.approved = false
        organization.createdAt = ZonedDateTime.now()

        val savedOrganization = organizationDao.save(organization)
        addUserToOrganization(organization.id, serviceRequest.owner.id, OrganizationRoleType.MEMBER)

        return savedOrganization
    }

    @Transactional(readOnly = true)
    override fun getAllOrganizations(): List<Organization> {
        return organizationDao.findAll()
    }

    @Transactional(readOnly = true)
    override fun findOrganizationById(id: Int): Organization? {
        return ServiceUtils.wrapOptional(organizationDao.findById(id))
    }

    @Transactional
    override fun approveOrganization(organizationId: Int, approve: Boolean, approvedBy: User): Organization {
        findOrganizationById(organizationId)?.let {
            it.approved = approve
            it.updatedAt = ZonedDateTime.now()
            it.approvedBy = approvedBy
            return it
        }
        throw ResourceNotFoundException("Missing organization with id: $organizationId")
    }

    @Transactional
    override fun addMemberToOrganization(user: User, organization: Organization): OrganizationMembership {
        return addUserToOrganization(user.id, organization.id, OrganizationRoleType.MEMBER)
    }

    @Transactional
    override fun addAdminToOrganization(user: User, organization: Organization): OrganizationMembership {
        return addUserToOrganization(user.id, organization.id, OrganizationRoleType.ADMIN)
    }

    @Transactional(readOnly = true)
    override fun findAllUsersFromOrganization(organizationId: Int): List<User> {
        return userDao.findAllUserForOrganization(organizationId)
    }

    @Transactional
    override fun findAllOrganizationsForUser(userId: Int): List<Organization> {
        return organizationDao.findAllOrganizationsForUser(userId)
    }

    private fun addUserToOrganization(
            organizationId: Int, userId: Int, role: OrganizationRoleType): OrganizationMembership {
        val membership = OrganizationMembership::class.java.newInstance()
        membership.organizationId = organizationId
        membership.userId = userId
        membership.role = getRole(role)
        membership.createdAt = ZonedDateTime.now()
        return membershipDao.save(membership)
    }

    private fun getRole(role: OrganizationRoleType): Role {
        return when(role) {
            OrganizationRoleType.ADMIN -> adminRole
            OrganizationRoleType.MEMBER -> memberRole
        }
    }
}
