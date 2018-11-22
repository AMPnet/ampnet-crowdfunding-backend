package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.request.UserUpdateRequest
import com.ampnet.crowdfundingbackend.controller.pojo.response.UserResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.UsersListResponse
import com.ampnet.crowdfundingbackend.enums.PrivilegeType
import com.ampnet.crowdfundingbackend.enums.UserRoleType
import com.ampnet.crowdfundingbackend.persistence.model.AuthMethod
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.security.WithMockCrowdfoundUser
import com.ampnet.crowdfundingbackend.service.UserService
import com.ampnet.crowdfundingbackend.service.pojo.CreateUserServiceRequest
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class UserControllerTest : ControllerTestBase() {

    private val pathUsers = "/users"
    private val pathMe = "/me"

    private lateinit var testUser: TestUser

    @Autowired
    private lateinit var userService: UserService

    @Before
    fun initTestData() {
        testUser = TestUser()
    }

    @Test
    @WithMockCrowdfoundUser(email = "test@test.com", privileges = [PrivilegeType.PRO_PROFILE])
    fun mustBeAbleToGetOwnProfile() {
        suppose("User exists in database") {
            databaseCleanerService.deleteAllUsers()
            testUser.email = "test@test.com"
            saveTestUser()
        }

        verify("The controller must return user data") {
            val result = mockMvc.perform(get(pathMe))
                    .andExpect(status().isOk)
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andReturn()
            val userResponse: UserResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(userResponse.email).isEqualTo(testUser.email)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_PROFILE])
    fun mustBeAbleToGetAListOfUsers() {
        suppose("Some user exists in database") {
            databaseCleanerService.deleteAllUsers()
            saveTestUser()
        }

        verify("The controller returns a list of users") {
            val result = mockMvc.perform(get(pathUsers))
                    .andExpect(status().isOk)
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andReturn()

            val listResponse: UsersListResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(listResponse.users).hasSize(1)
        }
    }

    @Test
    @WithMockCrowdfoundUser(role = UserRoleType.USER)
    fun mustNotBeAbleToGetAListOfUsersWithoutAdminPermission() {
        verify("The user with role USER cannot fetch a list of users") {
            mockMvc.perform(get(pathUsers))
                    .andExpect(status().isForbidden)
        }
    }

    @Test
    @WithMockCrowdfoundUser(completeProfile = false, email = "test@test.com")
    fun mustEnableFetchingOwnProfileForIncompleteUserProfile() {
        suppose("User with incomplete profile exists in database") {
            databaseCleanerService.deleteAllUsers()
            val user = CreateUserServiceRequest("test@test.com", null, null, null, null, null, AuthMethod.EMAIL)
            userService.create(user)
        }

        verify("The system returns user profile") {
            val result = mockMvc.perform(get(pathMe))
                    .andExpect(status().isOk)
                    .andReturn()

            val userResponse: UserResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(userResponse.email).isEqualTo("test@test.com")
            assertThat(userResponse.firstName).isNull()
            assertThat(userResponse.lastName).isNull()
            assertThat(userResponse.country).isNull()
            assertThat(userResponse.phoneNumber).isNull()
        }
    }

    @Test
    @WithMockCrowdfoundUser(completeProfile = false, role = UserRoleType.ADMIN)
    fun mustThrowErrorForIncompleteUserProfile() {
        verify("User with incomplete profile with get an error") {
            mockMvc.perform(get(pathUsers))
                    .andExpect(status().isConflict)
        }
    }

    @Test
    @WithMockCrowdfoundUser(completeProfile = false, email = "john@smith.com")
    fun mustBeAbleToUpdateProfile() {
        suppose("User exists in database") {
            databaseCleanerService.deleteAllUsers()
            saveTestUser()
        }

        verify("User can update his profile") {
            testUser.firstName = "NewFirstName"
            testUser.phoneNumber = "099123123"
            val request = getUpdateUserRequestFromTestUser()
            val result = mockMvc.perform(
                    post(pathMe)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk)
                    .andReturn()

            val userResponse: UserResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(userResponse.phoneNumber).isEqualTo(testUser.phoneNumber)
            assertThat(userResponse.firstName).isEqualTo(testUser.firstName)
            assertThat(userResponse.email).isEqualTo(testUser.email)
        }
        verify("User profile is updated in database") {
            val user = userService.find(testUser.email)
            assertThat(testUser).isNotNull
            assertThat(user!!.firstName).isEqualTo(testUser.firstName)
            assertThat(user.phoneNumber).isEqualTo(testUser.phoneNumber)
        }
    }

    @Test
    @WithMockCrowdfoundUser(email = "other@user.com")
    fun mustNotBeAbleToUpdateOthersUserProfile() {
        verify("User cannot update others profile") {
            val request = getUpdateUserRequestFromTestUser()
            mockMvc.perform(
                    post(pathMe)
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden)
        }
    }

    private fun saveTestUser(): User {
        val request = CreateUserServiceRequest(
                email = testUser.email,
                password = testUser.password,
                firstName = testUser.firstName,
                lastName = testUser.lastName,
                countryId = testUser.countryId,
                phoneNumber = testUser.phoneNumber,
                authMethod = testUser.authMethod
        )
        return userService.create(request)
    }

    private fun getUpdateUserRequestFromTestUser(): UserUpdateRequest {
        return UserUpdateRequest(
                testUser.email,
                testUser.firstName,
                testUser.lastName,
                testUser.countryId,
                testUser.phoneNumber
        )
    }

    private class TestUser {
        var email = "john@smith.com"
        var password = "Password157!"
        var firstName = "John"
        var lastName = "Smith"
        var countryId = 1
        var phoneNumber = "0951234567"
        var authMethod = AuthMethod.EMAIL
    }
}
