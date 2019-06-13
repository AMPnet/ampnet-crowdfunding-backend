package com.ampnet.crowdfundingbackend.service

class ProjectServiceTest : JpaServiceTestBase() {

//    private val cloudStorageService: CloudStorageServiceImpl = Mockito.mock(CloudStorageServiceImpl::class.java)
//
//    private val projectService: ProjectServiceImpl by lazy {
//        val storageServiceImpl = StorageServiceImpl(documentRepository, cloudStorageService)
//        ProjectServiceImpl(projectRepository, storageServiceImpl)
//    }
//    private val user: User by lazy {
//        databaseCleanerService.deleteAllUsers()
//        createUser("test@email.com", "Test", "User")
//    }
//    private val imageContent = "data".toByteArray()
//
//    private lateinit var organization: Organization
//    private lateinit var testContext: TestContext
//
//    @BeforeEach
//    fun initTestContext() {
//        databaseCleanerService.deleteAllWalletsAndOwners()
//        organization = createOrganization("Das Organization", user)
//        testContext = TestContext()
//    }
//
//    @Test
//    fun mustBeAbleToCreateProject() {
//        suppose("Organization has a wallet") {
//            createWalletForOrganization(organization,
//                "0xc5825e732eda043b83ea19a3a1bd2f27a65d11d6e887fa52763bb069977aa292")
//        }
//        suppose("Service received a request to create a project") {
//            databaseCleanerService.deleteAllProjects()
//            testContext.createProjectRequest = createProjectRequest("Test project")
//            testContext.project = projectService.createProject(testContext.createProjectRequest)
//        }
//
//        verify("Project is created") {
//            val optionalProject = projectRepository.findByIdWithOrganizationAndCreator(testContext.project.id)
//            assertThat(optionalProject).isPresent
//            val project = optionalProject.get()
//            val request = testContext.createProjectRequest
//            assertAll()
//            assertThat(project.name).isEqualTo(request.name)
//            assertThat(project.description).isEqualTo(request.description)
//            assertThat(project.location).isEqualTo(request.location)
//            assertThat(project.locationText).isEqualTo(request.locationText)
//            assertThat(project.returnOnInvestment).isEqualTo(request.returnOnInvestment)
//            assertThat(project.startDate).isEqualTo(request.startDate)
//            assertThat(project.endDate).isEqualTo(request.endDate)
//            assertThat(project.expectedFunding).isEqualTo(request.expectedFunding)
//            assertThat(project.currency).isEqualTo(request.currency)
//            assertThat(project.minPerUser).isEqualTo(request.minPerUser)
//            assertThat(project.maxPerUser).isEqualTo(request.maxPerUser)
//            assertThat(project.createdBy.id).isEqualTo(request.createdBy.id)
//            assertThat(project.organization.id).isEqualTo(organization.id)
//            assertThat(project.active).isEqualTo(request.active)
//            assertThat(project.mainImage.isNullOrEmpty()).isTrue()
//            assertThat(project.gallery.isNullOrEmpty()).isTrue()
//            assertThat(project.documents.isNullOrEmpty()).isTrue()
//        }
//    }
//
//    @Test
//    fun mustNotBeAbleToCreateProjectWithoutOrganizationWallet() {
//        verify("Service will throw an exception") {
//            databaseCleanerService.deleteAllProjects()
//            testContext.createProjectRequest = createProjectRequest("Test project")
//
//            val exception = assertThrows<InvalidRequestException> {
//                projectService.createProject(testContext.createProjectRequest)
//            }
//            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_MISSING)
//        }
//    }
//
//    @Test
//    fun mustNotBeAbleToCreateProjectWithEndDateBeforeStartDate() {
//        suppose("Request has end date before start date") {
//            testContext.createProjectRequest = CreateProjectServiceRequest(
//                    organization,
//                    "Invalid date",
//                    "Description",
//                    "location",
//                    "locationText",
//                    "1-2%",
//                    ZonedDateTime.now(),
//                    ZonedDateTime.now().minusDays(1),
//                    1000000,
//                    Currency.EUR,
//                    100,
//                    10000,
//                    false,
//                    user
//            )
//        }
//
//        verify("Service will throw an exception") {
//            val exception = assertThrows<InvalidRequestException> {
//                projectService.createProject(testContext.createProjectRequest)
//            }
//            assertThat(exception.errorCode).isEqualTo(ErrorCode.PRJ_DATE)
//        }
//    }
//
//    @Test
//    fun mustBeAbleToAddMainImage() {
//        suppose("Organization has a wallet") {
//            createWalletForOrganization(organization,
//                "0xc5825e732eda043b83ea19a3a1bd2f27a65d11d6e887fa52763bb069977aa292")
//        }
//        suppose("Project exists") {
//            databaseCleanerService.deleteAllProjects()
//            testContext.createProjectRequest = createProjectRequest("Image")
//            testContext.project = projectService.createProject(testContext.createProjectRequest)
//        }
//        suppose("Main image is added to project") {
//            testContext.imageLink = "link-main-image"
//            Mockito.`when`(
//                    cloudStorageService.saveFile(testContext.imageLink, imageContent)
//            ).thenReturn(testContext.imageLink)
//            projectService.addMainImage(testContext.project, testContext.imageLink, imageContent)
//        }
//
//        verify("Image is stored in project") {
//            val optionalProject = projectRepository.findById(testContext.project.id)
//            assertThat(optionalProject).isPresent
//            assertThat(optionalProject.get().mainImage).isEqualTo(testContext.imageLink)
//        }
//    }
//
//    @Test
//    fun mustBeAbleToAddImagesToGallery() {
//        suppose("Organization has a wallet") {
//            createWalletForOrganization(organization,
//                "0xc5825e732eda043b83ea19a3a1bd2f27a65d11d6e887fa52763bb069977aa292")
//        }
//        suppose("Project exists") {
//            databaseCleanerService.deleteAllProjects()
//            testContext.createProjectRequest = createProjectRequest("Image")
//            testContext.project = projectService.createProject(testContext.createProjectRequest)
//        }
//        suppose("Two images are added to project gallery") {
//            testContext.gallery = listOf("link-1", "link-2")
//            testContext.gallery.forEach {
//                Mockito.`when`(cloudStorageService.saveFile(it, imageContent)).thenReturn(it)
//                projectService.addImageToGallery(testContext.project, it, imageContent)
//            }
//        }
//
//        verify("The project gallery contains added images") {
//            val optionalProject = projectRepository.findById(testContext.project.id)
//            assertThat(optionalProject).isPresent
//            assertThat(optionalProject.get().gallery).containsAll(testContext.gallery)
//        }
//    }
//
//    @Test
//    fun mustBeAbleToAppendNewImageToGallery() {
//        suppose("Organization has a wallet") {
//            createWalletForOrganization(organization,
//                "0xc5825e732eda043b83ea19a3a1bd2f27a65d11d6e887fa52763bb069977aa292")
//        }
//        suppose("Project exists") {
//            databaseCleanerService.deleteAllProjects()
//            testContext.createProjectRequest = createProjectRequest("Image")
//            testContext.project = projectService.createProject(testContext.createProjectRequest)
//        }
//        suppose("The has gallery") {
//            testContext.gallery = listOf("link-1", "link-2")
//            testContext.project.gallery = testContext.gallery
//            projectRepository.save(testContext.project)
//        }
//        suppose("Additional image is added to gallery") {
//            testContext.imageLink = "link-new"
//            Mockito.`when`(
//                    cloudStorageService.saveFile(testContext.imageLink, imageContent)
//            ).thenReturn(testContext.imageLink)
//            projectService.addImageToGallery(testContext.project, testContext.imageLink, imageContent)
//        }
//
//        verify("Gallery has additional image") {
//            val optionalProject = projectRepository.findById(testContext.project.id)
//            assertThat(optionalProject).isPresent
//            val gallery = optionalProject.get().gallery
//            assertThat(gallery).containsAll(testContext.gallery)
//            assertThat(gallery).contains(testContext.imageLink)
//        }
//    }
//
//    @Test
//    fun mustBeAbleToRemoveImageFromGallery() {
//        suppose("Organization has a wallet") {
//            createWalletForOrganization(organization,
//                    "0xc5825e732eda043b83ea19a3a1bd2f27a65d11d6e887fa52763bb069977aa292")
//        }
//        suppose("Project exists") {
//            databaseCleanerService.deleteAllProjects()
//            testContext.createProjectRequest = createProjectRequest("Image")
//            testContext.project = projectService.createProject(testContext.createProjectRequest)
//        }
//        suppose("The has gallery") {
//            testContext.gallery = listOf("link-1", "link-2", "link-3")
//            testContext.project.gallery = testContext.gallery
//            projectRepository.save(testContext.project)
//        }
//        suppose("Image is removed from gallery") {
//            projectService.removeImagesFromGallery(testContext.project, listOf("link-1", "link-3"))
//        }
//
//        verify("Gallery has additional image") {
//            val optionalProject = projectRepository.findById(testContext.project.id)
//            assertThat(optionalProject).isPresent
//            val gallery = optionalProject.get().gallery
//            assertThat(gallery).doesNotContain("link-1", "link-3")
//            assertThat(gallery).contains("link-2")
//        }
//    }
//
//    @Test
//    fun mustNotBeAbleToSetEndDateBeforePresent() {
//        suppose("Request has end date before present date") {
//            val currentTime = ZonedDateTime.now()
//            testContext.createProjectRequest = CreateProjectServiceRequest(
//                    organization,
//                    "Invalid end date",
//                    "Description",
//                    "location",
//                    "locationText",
//                    "1-2%",
//                    currentTime.minusDays(60),
//                    currentTime.minusDays(30),
//                    1000000,
//                    Currency.EUR,
//                    100,
//                    10000,
//                    false,
//                    user
//            )
//        }
//
//        verify("Service will throw an exception") {
//            val exception = assertThrows<InvalidRequestException> {
//                projectService.createProject(testContext.createProjectRequest)
//            }
//            assertThat(exception.errorCode).isEqualTo(ErrorCode.PRJ_DATE)
//        }
//    }
//
//    @Test
//    fun mustNotBeAbleToSetMinPerUserAboveMaxPerUser() {
//        suppose("Request has min per user value above max per user") {
//            val currentTime = ZonedDateTime.now()
//            testContext.createProjectRequest = CreateProjectServiceRequest(
//                    organization,
//                    "Invalid end date",
//                    "Description",
//                    "location",
//                    "locationText",
//                    "1-2%",
//                    currentTime,
//                    currentTime.plusDays(30),
//                    1000000,
//                    Currency.EUR,
//                    1_000,
//                    1,
//                    false,
//                    user
//            )
//        }
//
//        verify("Service will throw an exception") {
//            val exception = assertThrows<InvalidRequestException> {
//                projectService.createProject(testContext.createProjectRequest)
//            }
//            assertThat(exception.errorCode).isEqualTo(ErrorCode.PRJ_MIN_ABOVE_MAX)
//        }
//    }
//
//    @Test
//    fun mustNotBeAbleToSetMaxPerUserAboveSystemMax() {
//        suppose("Request has max per user value above system max") {
//            val currentTime = ZonedDateTime.now()
//            testContext.createProjectRequest = CreateProjectServiceRequest(
//                    organization,
//                    "Invalid end date",
//                    "Description",
//                    "location",
//                    "locationText",
//                    "1-2%",
//                    currentTime,
//                    currentTime.plusDays(30),
//                    10_000_000_000_000,
//                    Currency.EUR,
//                    1,
//                    projectService.maxPerUserInvestment + 1,
//                    false,
//                    user
//            )
//        }
//
//        verify("Service will throw an exception") {
//            val exception = assertThrows<InvalidRequestException> {
//                projectService.createProject(testContext.createProjectRequest)
//            }
//            assertThat(exception.errorCode).isEqualTo(ErrorCode.PRJ_MAX_FUNDS_PER_USER_TOO_HIGH)
//        }
//    }
//
//    @Test
//    fun mustNotBeAbleToSetExpectedFundingAboveSystemMax() {
//        suppose("Request has max per user value above system max") {
//            val currentTime = ZonedDateTime.now()
//            testContext.createProjectRequest = CreateProjectServiceRequest(
//                    organization,
//                    "Invalid end date",
//                    "Description",
//                    "location",
//                    "locationText",
//                    "1-2%",
//                    currentTime,
//                    currentTime.plusDays(30),
//                    projectService.maxProjectInvestment + 1,
//                    Currency.EUR,
//                    1,
//                    1_000_000_000,
//                    false,
//                    user
//            )
//        }
//
//        verify("Service will throw an exception") {
//            val exception = assertThrows<InvalidRequestException> {
//                projectService.createProject(testContext.createProjectRequest)
//            }
//            assertThat(exception.errorCode).isEqualTo(ErrorCode.PRJ_MAX_FUNDS_TOO_HIGH)
//        }
//    }
//
//    @Test
//    fun mustNotBeAbleToRemoveDocumentFromNonExistingProject() {
//        verify("Service will throw an exception") {
//            val exception = assertThrows<ResourceNotFoundException> {
//                projectService.removeDocument(0, 0)
//            }
//            assertThat(exception.errorCode).isEqualTo(ErrorCode.PRJ_MISSING)
//        }
//    }
//
//    private fun createProjectRequest(name: String): CreateProjectServiceRequest {
//        return CreateProjectServiceRequest(
//                organization,
//                name,
//                "Description",
//                "location",
//                "locationText",
//                "1-2%",
//                ZonedDateTime.now(),
//                ZonedDateTime.now().plusDays(30),
//                1000000,
//                Currency.EUR,
//                100,
//                10000,
//                false,
//                user
//        )
//    }
//
//    private class TestContext {
//        lateinit var createProjectRequest: CreateProjectServiceRequest
//        lateinit var project: Project
//        lateinit var imageLink: String
//        lateinit var gallery: List<String>
//    }
}
