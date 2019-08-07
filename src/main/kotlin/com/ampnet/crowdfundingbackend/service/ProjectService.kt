package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.controller.pojo.request.ProjectUpdateRequest
import com.ampnet.crowdfundingbackend.persistence.model.Document
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.service.pojo.CreateProjectServiceRequest
import com.ampnet.crowdfundingbackend.service.pojo.DocumentSaveRequest

interface ProjectService {
    fun createProject(request: CreateProjectServiceRequest): Project
    fun getProjectById(id: Int): Project?
    fun getProjectByIdWithWallet(id: Int): Project?
    fun getProjectByIdWithAllData(id: Int): Project?
    fun getAllProjectsForOrganization(organizationId: Int): List<Project>
    fun getAllProjects(): List<Project>
    fun getAllActiveWithWallet(): List<Project>
    fun updateProject(project: Project, request: ProjectUpdateRequest): Project

    fun addMainImage(project: Project, name: String, content: ByteArray)
    fun addImageToGallery(project: Project, name: String, content: ByteArray)
    fun removeImagesFromGallery(project: Project, images: List<String>)
    fun addDocument(project: Project, request: DocumentSaveRequest): Document
    fun removeDocument(project: Project, documentId: Int)
    fun addNews(project: Project, link: String)
    fun removeNews(project: Project, link: String)
}
