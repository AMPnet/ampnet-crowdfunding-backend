package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.service.pojo.CreateProjectServiceRequest

interface ProjectService {
    fun createProject(request: CreateProjectServiceRequest): Project
    fun getProjectById(id: Int): Project?
    fun addMainImage(project: Project, mainImage: String)
    fun addImagesToGallery(project: Project, images: List<String>)
}
