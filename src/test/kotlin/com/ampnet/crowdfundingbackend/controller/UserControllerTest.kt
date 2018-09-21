package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.TestBase
import com.ampnet.crowdfundingbackend.controller.pojo.response.UserResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.UsersResponse
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
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class UserControllerTest : TestBase() {

    private val pathUsers = "/users"

    private lateinit var testContext: TestContext

    @Autowired
    private lateinit var userService: UserService

    @Before
    fun initContext() {
        testContext = TestContext()
    }

    @Test
    @WithMockCrowdfoundUser
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
    fun mustNotBeAbleToGetAListOfUsersWithUserRole() {
        verify("The user with role USER cannot fetch a list of users") {
            mockMvc.perform(get(pathUsers))
                    .andExpect(status().isForbidden)
        }
    }

    @Test
    fun mustBeAbleToSignUpUser() {
        val email = "user@gmail.com"

        suppose("The user send request to sign up") {
            val requestJson = """
            |{
                |"signup_method" : "EMAIL",
                |"user_info" : {
                    |"email" : "$email",
                    |"password" : "password"
                |}
            |}""".trimMargin()

            testContext.result = mockMvc.perform(
                    post("/signup")
                            .content(requestJson)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk)
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andReturn()
        }

        verify("The controller returned valid user") {
            val userResponse: UserResponse = objectMapper.readValue(testContext.result.response.contentAsString)
            assertThat(userResponse.email).isEqualTo(email)
        }
        verify("The user is stored in database") {
            val optionalUserInRepo = userService.find(email)
            assertThat(optionalUserInRepo.isPresent).isTrue()
        }
    }

    private fun createTestUsers(email: String): User {
        val request = CreateUserServiceRequest(email, "password", AuthMethod.EMAIL)
        return userService.create(request)
    }

    private class TestContext {
        lateinit var result: MvcResult
    }
}
