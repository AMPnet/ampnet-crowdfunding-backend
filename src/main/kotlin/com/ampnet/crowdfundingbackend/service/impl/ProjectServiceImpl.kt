package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.InvalidRequestException
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.repository.ProjectRepository
import com.ampnet.crowdfundingbackend.service.ProjectService
import com.ampnet.crowdfundingbackend.service.pojo.CreateProjectServiceRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Service
class ProjectServiceImpl(private val projectRepository: ProjectRepository) : ProjectService {

    @Transactional
    override fun createProject(request: CreateProjectServiceRequest): Project {
        if (request.endDate.isBefore(request.startDate)) {
            throw InvalidRequestException(ErrorCode.PRJ_DATE, "End date cannot be before start date")
        }

        val project = createProjectFromRequest(request)
        project.createdAt = ZonedDateTime.now()
        project.active = false
        return projectRepository.save(project)
    }

    @Transactional(readOnly = true)
    override fun getProjectById(id: Int): Project? {
        return ServiceUtils.wrapOptional(projectRepository.findByIdWithOrganization(id))
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
        project.expectedFunding = request.expectedFounding
        project.currency = request.currency
        project.minPerUser = request.minPerUser
        project.maxPerUser = request.maxPerUser
        project.createdBy = request.createdBy
//        project.mainImage = request.mainImage
//        project.gallery = request.gallery
//        project.documents = request.documents
        return project
    }
}
