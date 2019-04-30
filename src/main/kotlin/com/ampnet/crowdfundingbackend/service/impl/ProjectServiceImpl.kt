package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.InvalidRequestException
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.persistence.model.Document
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.repository.ProjectRepository
import com.ampnet.crowdfundingbackend.service.StorageService
import com.ampnet.crowdfundingbackend.service.ProjectService
import com.ampnet.crowdfundingbackend.service.pojo.CreateProjectServiceRequest
import com.ampnet.crowdfundingbackend.service.pojo.DocumentSaveRequest
import mu.KLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Service
class ProjectServiceImpl(
    private val projectRepository: ProjectRepository,
    private val storageService: StorageService
) : ProjectService {

    companion object : KLogging()

    val maxProjectInvestment: Long = 100_000_000_000_000_00
    val maxPerUserInvestment: Long = 1_000_000_000_000_00

    @Transactional
    override fun createProject(request: CreateProjectServiceRequest): Project {
        validateCreateProjectRequest(request)
        if (request.organization.wallet == null) {
            logger.info { "Trying to create project without organization wallet. " +
                "Organization: ${request.organization.id}"
            }
            throw InvalidRequestException(ErrorCode.WALLET_MISSING,
                "Organization cannot create project without organization wallet")
        }

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

    @Transactional(readOnly = true)
    override fun getProjectByIdWithAllData(id: Int): Project? {
        return ServiceUtils.wrapOptional(projectRepository.findByIdWithAllData(id))
    }

    @Transactional(readOnly = true)
    override fun getAllProjectsForOrganization(organizationId: Int): List<Project> {
        return projectRepository.findAllByOrganizationId(organizationId)
    }

    @Transactional
    override fun addMainImage(project: Project, name: String, content: ByteArray) {
        val link = storageService.saveImage(name, content)
        project.mainImage = link
        projectRepository.save(project)
    }

    @Transactional
    override fun addImageToGallery(project: Project, name: String, content: ByteArray) {
        val gallery = project.gallery.orEmpty().toMutableList()
        val link = storageService.saveImage(name, content)
        gallery.add(link)
        setProjectGallery(project, gallery)
    }

    @Transactional
    override fun removeImagesFromGallery(project: Project, images: List<String>) {
        val gallery = project.gallery.orEmpty().toMutableList()
        images.forEach {
            if (gallery.remove(it)) {
                storageService.deleteImage(it)
            }
        }
        setProjectGallery(project, gallery)
    }

    @Transactional
    override fun addDocument(projectId: Int, request: DocumentSaveRequest): Document {
        val project = projectRepository.findById(projectId).orElseThrow {
            logger.info { "Trying to add document to missing project. Project: $projectId" }
            throw ResourceNotFoundException(ErrorCode.PRJ_MISSING, "Missing project: $projectId")
        }

        val document = storageService.saveDocument(request)
        addDocumentToProject(project, document)
        return document
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

    private fun addDocumentToProject(project: Project, document: Document) {
        val documents = project.documents.orEmpty().toMutableList()
        documents += document
        project.documents = documents
        projectRepository.save(project)
    }

    private fun createProjectFromRequest(request: CreateProjectServiceRequest): Project {
        val project = Project::class.java.newInstance()
        project.organization = request.organization
        project.name = request.name
        project.description = request.description
        project.location = request.location
        project.locationText = request.locationText
        project.returnOnInvestment = request.returnOnInvestment
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

    private fun setProjectGallery(project: Project, gallery: List<String>) {
        project.gallery = gallery
        projectRepository.save(project)
    }
}
