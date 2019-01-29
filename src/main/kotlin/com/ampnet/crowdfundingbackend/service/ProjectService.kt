package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.persistence.model.Document
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.service.pojo.CreateProjectServiceRequest
import com.ampnet.crowdfundingbackend.service.pojo.DocumentSaveRequest

interface ProjectService {
    fun createProject(request: CreateProjectServiceRequest): Project
    fun getProjectById(id: Int): Project?
    fun getProjectByIdWithWallet(id: Int): Project?
    fun getAllProjectsForOrganization(organizationId: Int): List<Project>
    fun addMainImage(project: Project, mainImage: String)
    fun addImagesToGallery(project: Project, images: List<String>)
    fun addDocument(projectId: Int, request: DocumentSaveRequest): Document
}
