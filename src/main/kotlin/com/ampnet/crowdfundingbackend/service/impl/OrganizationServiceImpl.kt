package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.enums.OrganizationRoleType
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.OrganizationFollower
import com.ampnet.crowdfundingbackend.persistence.model.OrganizationMembership
import com.ampnet.crowdfundingbackend.persistence.model.Role
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.persistence.repository.OrganizationDao
import com.ampnet.crowdfundingbackend.persistence.repository.OrganizationFollowerDao
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
    private val followerDao: OrganizationFollowerDao,
    private val roleDao: RoleDao,
    private val userDao: UserDao
) : OrganizationService {

    private val adminRole: Role by lazy { roleDao.getOne(OrganizationRoleType.ORG_ADMIN.id) }
    private val memberRole: Role by lazy { roleDao.getOne(OrganizationRoleType.ORG_MEMBER.id) }

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
        addUserToOrganization(serviceRequest.owner.id, organization.id, OrganizationRoleType.ORG_ADMIN)

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

    @Transactional(readOnly = true)
    override fun findAllUsersFromOrganization(organizationId: Int): List<User> {
        return userDao.findAllUserForOrganization(organizationId)
    }

    @Transactional(readOnly = true)
    override fun findAllOrganizationsForUser(userId: Int): List<Organization> {
        return organizationDao.findAllOrganizationsForUser(userId)
    }

    @Transactional(readOnly = true)
    override fun getOrganizationMemberships(organizationId: Int): List<OrganizationMembership> {
        return membershipDao.findByOrganizationId(organizationId)
    }

    @Transactional
    override fun addUserToOrganization(
        userId: Int,
        organizationId: Int,
        role: OrganizationRoleType
    ): OrganizationMembership {
        // user can have only one membership(role) per one organization
        val membership = ServiceUtils.wrapOptional(membershipDao.findByOrganizationIdAndUserId(organizationId, userId))
                ?: OrganizationMembership::class.java.newInstance()

        membership.organizationId = organizationId
        membership.userId = userId
        membership.role = getRole(role)
        membership.createdAt = ZonedDateTime.now()
        return membershipDao.save(membership)
    }

    @Transactional
    override fun followOrganization(userId: Int, organizationId: Int): OrganizationFollower {
        ServiceUtils.wrapOptional(followerDao.findByUserIdAndOrganizationId(userId, organizationId))?.let {
            return it
        }
        val follower = OrganizationFollower::class.java.newInstance()
        follower.userId = userId
        follower.organizationId = organizationId
        follower.createdAt = ZonedDateTime.now()
        return followerDao.save(follower)
    }

    @Transactional
    override fun unfollowOrganization(userId: Int, organizationId: Int) {
        ServiceUtils.wrapOptional(followerDao.findByUserIdAndOrganizationId(userId, organizationId))?.let {
            followerDao.delete(it)
        }
    }

    private fun getRole(role: OrganizationRoleType): Role {
        return when (role) {
            OrganizationRoleType.ORG_ADMIN -> adminRole
            OrganizationRoleType.ORG_MEMBER -> memberRole
        }
    }
}
