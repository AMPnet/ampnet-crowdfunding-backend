package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.config.DatabaseCleanerService
import com.ampnet.crowdfundingbackend.config.auth.TokenProvider
import com.ampnet.crowdfundingbackend.config.auth.UserPrincipal
import com.ampnet.crowdfundingbackend.controller.pojo.response.AuthTokenResponse
import com.ampnet.crowdfundingbackend.exception.ErrorResponse
import com.ampnet.crowdfundingbackend.exception.InvalidLoginMethodException
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.persistence.model.AuthMethod
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.service.SocialService
import com.ampnet.crowdfundingbackend.service.UserService
import com.ampnet.crowdfundingbackend.service.pojo.CreateUserServiceRequest
import com.ampnet.crowdfundingbackend.service.pojo.SocialUser
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.hasKey
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@ActiveProfiles("SocialMockConfig")
class AuthenticationControllerTest : ControllerTestBase() {

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var tokenProvider: TokenProvider

    @Autowired
    private lateinit var socialService: SocialService

    @Autowired
    private lateinit var databaseCleanerService: DatabaseCleanerService

    private lateinit var result: MvcResult
    private lateinit var user: User

    private val tokenPath = "/token"
    private val regularTestUser = RegularTestUser()
    private val facebookTestUser = FacebookTestUser()
    private val googleTestUser = GoogleTestUser()

    @Before
    fun clearDatabase() {
        databaseCleanerService.deleteAll()
    }

    @Test
    fun signInRegular() {
        suppose("User exists in database.") {
            user = userService.create(CreateUserServiceRequest(
                    email = regularTestUser.email,
                    password = regularTestUser.password,
                    firstName = regularTestUser.firstName,
                    lastName = regularTestUser.lastName,
                    countryId = regularTestUser.countryId,
                    phoneNumber = regularTestUser.phoneNumber,
                    authMethod = regularTestUser.authMethod
            ))
        }
        verify("User can fetch token with valid credentials.") {
            val requestBody = """
                |{
                |  "login_method" : "${regularTestUser.authMethod}",
                |  "credentials" : {
                |    "email" : "${regularTestUser.email}",
                |    "password" : "${regularTestUser.password}"
                |  }
                |}
            """.trimMargin()
            result = mockMvc.perform(
                    post(tokenPath)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk)
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andExpect(jsonPath("$").value(hasKey("token")))
                    .andReturn()
        }
        verify("Token is valid.") {
            val response = objectMapper.readValue<AuthTokenResponse>(result.response.contentAsString)
            verifyTokenForUserData(response.token)
        }
    }

    @Test
    fun signInFacebook() {
        suppose("Social service is mocked to return valid Facebook user.") {
            Mockito.`when`(socialService.getFacebookUserInfo(facebookTestUser.fbToken))
                    .thenReturn(facebookTestUser.socialUser)
        }
        suppose("Social user identified by Facebook exists in our database.") {
            user = userService.create(
                    CreateUserServiceRequest(
                            facebookTestUser.socialUser,
                            facebookTestUser.authMethod
                    )
            )
        }
        verify("User can fetch token with valid credentials.") {
            val requestBody = """
                |{
                |  "login_method" : "${facebookTestUser.authMethod}",
                |  "credentials" : {
                |    "token" : "${facebookTestUser.fbToken}"
                |  }
                |}
            """.trimMargin()
            result = mockMvc.perform(
                    post(tokenPath)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk)
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andExpect(jsonPath("$").value(hasKey("token")))
                    .andReturn()
        }
        verify("Token is valid.") {
            val response = objectMapper.readValue<AuthTokenResponse>(result.response.contentAsString)
            verifyTokenForUserData(response.token)
        }
    }

    @Test
    fun signInGoogle() {
        suppose("Social service is mocked to return valid Google user.") {
            Mockito.`when`(socialService.getGoogleUserInfo(googleTestUser.googleToken))
                    .thenReturn(googleTestUser.socialUser)
        }
        suppose("Social user identified by Facebook exists in our database.") {
            user = userService.create(
                    CreateUserServiceRequest(
                            googleTestUser.socialUser,
                            googleTestUser.authMethod
                    )
            )
        }
        verify("User can fetch token with valid credentials.") {
            val requestBody = """
                |{
                |  "login_method" : "${googleTestUser.authMethod}",
                |  "credentials" : {
                |    "token" : "${googleTestUser.googleToken}"
                |  }
                |}
            """.trimMargin()
            result = mockMvc.perform(
                    post(tokenPath)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk)
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andExpect(jsonPath("$").value(hasKey("token")))
                    .andReturn()
        }
        verify("Token is valid.") {
            val response = objectMapper.readValue<AuthTokenResponse>(result.response.contentAsString)
            verifyTokenForUserData(response.token)
        }
    }

    @Test
    fun signInWithInvalidCredentialsShouldFail() {
        suppose("User with email ${regularTestUser.email} exists in database.") {
            user = userService.create(CreateUserServiceRequest(
                    email = regularTestUser.email,
                    password = regularTestUser.password,
                    firstName = regularTestUser.firstName,
                    lastName = regularTestUser.lastName,
                    countryId = regularTestUser.countryId,
                    phoneNumber = regularTestUser.phoneNumber,
                    authMethod = regularTestUser.authMethod
            ))
        }
        verify("User cannot fetch token with invalid credentials") {
            val requestBody = """
                |{
                |  "login_method" : "${regularTestUser.authMethod}",
                |  "credentials" : {
                |    "email" : "${regularTestUser.email}",
                |    "password" : "wrong-password"
                |  }
                |}
            """.trimMargin()
            mockMvc.perform(
                    post(tokenPath)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                    .andExpect(status().isUnauthorized)
        }
    }

    @Test
    fun signInWithNonExistingUserShouldFail() {
        suppose("User with email ${regularTestUser.email} does not exist in database.") {
            val user = userService.find(regularTestUser.email)
            assert(!user.isPresent)
        }
        verify("User cannot fetch token without signing up first.") {
            val requestBody = """
                |{
                |  "login_method" : "${regularTestUser.authMethod}",
                |  "credentials" : {
                |    "email" : "${regularTestUser.email}",
                |    "password" : "${regularTestUser.password}"
                |  }
                |}
            """.trimMargin()
            result = mockMvc.perform(
                    post(tokenPath)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest)
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andReturn()
            val error = objectMapper.readValue<ErrorResponse>(result.response.contentAsString)
            assert(error.reason == ResourceNotFoundException::class.java.canonicalName)
        }
    }

    @Test
    fun signInWithInvalidLoginMethodShouldFail() {
        suppose("User exists in database, created by regular registration.") {
            userService.create(CreateUserServiceRequest(
                    email = regularTestUser.email,
                    password = regularTestUser.password,
                    firstName = regularTestUser.firstName,
                    lastName = regularTestUser.lastName,
                    countryId = regularTestUser.countryId,
                    phoneNumber = regularTestUser.phoneNumber,
                    authMethod = regularTestUser.authMethod
            ))
        }
        suppose("Social service is mocked to return google user with same email as user registered in regular way.") {
            Mockito.`when`(socialService.getGoogleUserInfo(googleTestUser.googleToken))
                    .thenReturn(googleTestUser.socialUser)
        }
        verify("The user cannot login using social method.") {
            val requestBody = """
                |{
                |  "login_method" : "${googleTestUser.authMethod}",
                |  "credentials" : {
                |      "token" : "${googleTestUser.googleToken}"
                |  }
                |}
            """.trimMargin()
            result = mockMvc.perform(
                    post(tokenPath)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest)
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andReturn()
            val errorResponse = objectMapper.readValue<ErrorResponse>(result.response.contentAsString)
            assert(errorResponse.reason == InvalidLoginMethodException::class.java.canonicalName)
        }
    }

    private fun verifyTokenForUserData(token: String) {
        val tokenPrincipal = tokenProvider.getAuthentication(token).principal as UserPrincipal
        val storedUserPrincipal = UserPrincipal(user)
        assertThat(tokenPrincipal).isEqualTo(storedUserPrincipal)
    }

    private class RegularTestUser {
        val email = "john@smith.com"
        val password = "Password175!"
        val firstName = "John"
        val lastName = "Smith"
        val countryId = 1
        val phoneNumber = "095123456"
        val authMethod = AuthMethod.EMAIL
    }

    private class FacebookTestUser {
        val socialUser = SocialUser(
                "john@smith.com",
                "John",
                "Smith",
                1
        )
        val fbToken = "token"
        val authMethod = AuthMethod.FACEBOOK
    }

    private class GoogleTestUser {
        val socialUser = SocialUser(
                "john@smith.com",
                "John",
                "Smith",
                null
        )
        val googleToken = "token"
        val authMethod = AuthMethod.GOOGLE
    }
}