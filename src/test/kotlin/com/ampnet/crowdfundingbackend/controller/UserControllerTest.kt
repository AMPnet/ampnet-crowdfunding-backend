package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.TestBase
import com.ampnet.crowdfundingbackend.controller.pojo.request.SignupRequest
import com.ampnet.crowdfundingbackend.controller.pojo.response.UserResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.UsersResponse
import com.ampnet.crowdfundingbackend.enums.PrivilegeType
import com.ampnet.crowdfundingbackend.enums.UserRoleType
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.security.WithMockCrowdfoundUser
import com.ampnet.crowdfundingbackend.service.UserService
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class UserControllerTest : TestBase() {

    private val pathUsers = "/users"

    @Autowired
    private lateinit var userService: UserService

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_PROFILE])
    fun mustBeAbleToGetAListOfUsers() {
        suppose("Some user exists in database") {
            createTestUsers("tester1")
        }

        verify("The controller returns a list of users") {
            val result = mockMvc.perform(get(pathUsers))
                    .andExpect(status().isOk)
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andReturn()

            val response: UsersResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(response.users).hasSize(1)
        }
    }

    @Test
    @WithMockCrowdfoundUser(role = UserRoleType.USER)
    fun mustNotBeAbleToGetAListOfUsersWithAdminPermission() {
        verify("The user with role USER cannot fetch a list of users") {
            mockMvc.perform(get(pathUsers))
                    .andExpect(status().isForbidden)
        }
    }

    @Test
    fun mustBeAbleToSignUpUser() {
        val email = "userX"
        lateinit var result: MvcResult

        suppose("The user send request to sign up") {
            val requestJson = """{
            |"email": "$email",
            |"password": "password",
            |"age": 0,
            |"salary": 0
            |}""".trimMargin()

            result = mockMvc.perform(
                    post("/signup")
                            .content(requestJson)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk)
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andReturn()
        }

        verify("The controller returned valid user") {
            val userResponse: UserResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(userResponse.email).isEqualTo(email)

        }
        verify("The user is stored in database") {
            val optionalUserInRepo = userService.find(email)
            assertThat(optionalUserInRepo.isPresent).isTrue()
            // TODO: verify user values with request values
        }
    }

    private fun createTestUsers(email: String): User {
        val request = SignupRequest(email, "password", 0, 0)
        return userService.create(request)
    }
}
