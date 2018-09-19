package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.TestBase
import com.ampnet.crowdfundingbackend.controller.pojo.SignupUserRequest
import com.ampnet.crowdfundingbackend.controller.pojo.UserResponse
import com.ampnet.crowdfundingbackend.controller.pojo.UsersResponse
import com.ampnet.crowdfundingbackend.enums.UserRoleType
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.security.WithMockCrowdfoundUser
import com.ampnet.crowdfundingbackend.service.UserService
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class UserControllerTest : TestBase() {

    private val pathUsers = "/users"

    @Autowired
    private lateinit var userService: UserService

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGetAListOfUsers() {
        createTestUsers("tester1")
        val result = mockMvc.perform(get(pathUsers))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andReturn()

        val response: UsersResponse = objectMapper.readValue(result.response.contentAsString)
        assertThat(response.users).hasSize(1)
    }

    @Test
    @WithMockCrowdfoundUser(role = UserRoleType.USER)
    fun mustNotBeAbleToGetAListOfUsersWithUserRole() {
        mockMvc.perform(get(pathUsers))
                .andExpect(status().isForbidden)
    }

    @Test
    fun mustBeAbleToSignUpUser() {
        val requestJson = """{
            |"username": "userX",
            |"password": "password",
            |"age": 0,
            |"salary": 0
            |}""".trimMargin()

        val result = mockMvc.perform(
                post("/signup")
                        .content(requestJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andReturn()

        val userResponse: UserResponse = objectMapper.readValue(result.response.contentAsString)
        assertThat(userResponse.username).isEqualTo("userX")
    }

    private fun createTestUsers(username: String): User {
        val request = SignupUserRequest(username, "password", 0, 0)
        return userService.create(request)
    }

}
