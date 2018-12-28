package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.InvalidRequestException
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.repository.ProjectRepository
import com.ampnet.crowdfundingbackend.service.ProjectService
import com.ampnet.crowdfundingbackend.service.pojo.CreateProjectServiceRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.ZonedDateTime

@Service
class ProjectServiceImpl(private val projectRepository: ProjectRepository) : ProjectService {

    private val maxProjectInvestment = BigDecimal(100_000_000_000_000)
    private val maxPerUserInvestment = BigDecimal(1_000_000_000_000)

    @Transactional
    override fun createProject(request: CreateProjectServiceRequest): Project {
        validateCreateProjectRequest(request)

        val project = createProjectFromRequest(request)
        project.createdAt = ZonedDateTime.now()
        return projectRepository.save(project)
    }

    @Transactional(readOnly = true)
    override fun getProjectById(id: Int): Project? {
        return ServiceUtils.wrapOptional(projectRepository.findByIdWithOrganization(id))
    }

    @Transactional(readOnly = true)
    override fun getProjectByIdWithWallet(id: Int): Project? {
        return ServiceUtils.wrapOptional(projectRepository.findByIdWithWallet(id))
    }

    @Transactional
    override fun addMainImage(project: Project, mainImage: String) {
        project.mainImage = mainImage
        projectRepository.save(project)
    }

    @Transactional
    override fun addImagesToGallery(project: Project, images: List<String>) {
        val gallery = project.gallery.orEmpty().toMutableList()
        gallery.addAll(images)
        project.gallery = gallery
        projectRepository.save(project)
    }

    private fun validateCreateProjectRequest(request: CreateProjectServiceRequest) {
        if (request.endDate.isBefore(request.startDate)) {
            throw InvalidRequestException(ErrorCode.PRJ_DATE, "End date cannot be before start date")
        }
        if (request.endDate.isBefore(ZonedDateTime.now())) {
            throw InvalidRequestException(ErrorCode.PRJ_DATE, "End date cannot be before present date")
        }
        if (request.minPerUser > request.maxPerUser) {
            throw InvalidRequestException(ErrorCode.PRJ_MIN_ABOVE_MAX,
                    "Min: ${request.minPerUser} > Max: ${request.maxPerUser}")
        }
        if (maxProjectInvestment <= request.expectedFunding) {
            throw InvalidRequestException(ErrorCode.PRJ_MAX_FUNDS_TOO_HIGH,
                    "Max expected funding is: $maxProjectInvestment")
        }
        if (maxPerUserInvestment <= request.maxPerUser) {
            throw InvalidRequestException(ErrorCode.PRJ_MAX_FUNDS_PER_USER_TOO_HIGH,
                    "Max funds per user is: $maxPerUserInvestment")
        }
    }

    private fun createProjectFromRequest(request: CreateProjectServiceRequest): Project {
        val project = Project::class.java.newInstance()
        project.organization = request.organization
        project.name = request.name
        project.description = request.description
        project.location = request.location
        project.locationText = request.locationText
        project.returnToInvestment = request.returnToInvestment
        project.startDate = request.startDate
        project.endDate = request.endDate
        project.expectedFunding = request.expectedFunding
        project.currency = request.currency
        project.minPerUser = request.minPerUser
        project.maxPerUser = request.maxPerUser
        project.createdBy = request.createdBy
        project.active = request.active
        return project
    }
}
