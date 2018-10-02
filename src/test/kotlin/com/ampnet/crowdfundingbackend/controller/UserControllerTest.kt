package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.TestBase
import com.ampnet.crowdfundingbackend.controller.pojo.response.UserResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.UsersResponse
import com.ampnet.crowdfundingbackend.enums.PrivilegeType
import com.ampnet.crowdfundingbackend.enums.UserRoleType
import com.ampnet.crowdfundingbackend.exception.ErrorResponse
import com.ampnet.crowdfundingbackend.exception.ResourceAlreadyExistsException
import com.ampnet.crowdfundingbackend.persistence.model.AuthMethod
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.security.WithMockCrowdfoundUser
import com.ampnet.crowdfundingbackend.service.DatabaseCleanerService
import com.ampnet.crowdfundingbackend.service.SocialService
import com.ampnet.crowdfundingbackend.service.UserService
import com.ampnet.crowdfundingbackend.service.pojo.CreateUserServiceRequest
import com.ampnet.crowdfundingbackend.service.pojo.SocialUser
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.ZonedDateTime

@ActiveProfiles("SocialMockConfig")
class UserControllerTest : TestBase() {

    private val pathUsers = "/users"
    private val pathSignup = "/signup"

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var socialService: SocialService

    @Autowired
    private lateinit var databaseCleanerService: DatabaseCleanerService

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

        databaseCleanerService.deleteAll()
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
    fun mustBeAbleToSignUpUser() {
        val email = "johnsmith@gmail.com"
        val password = "Password1578!"
        val firstName = "john"
        val lastName = "smith"
        val phoneNumber = "0951234567"
        val signupMethod = AuthMethod.EMAIL
        val countryId = 1

        lateinit var result: MvcResult

        suppose("The user send request to sign up") {
            val requestJson = generateSignupJson(
                    email = email,
                    password = password,
                    firstName = firstName,
                    lastName = lastName,
                    phoneNumber = phoneNumber,
                    countryId = countryId
            )
            result = mockMvc.perform(
                    post(pathSignup)
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
            val userInRepo = optionalUserInRepo.get()

            assert(userInRepo.email == email)
            assert(passwordEncoder.matches(password, userInRepo.password))
            assert(userInRepo.firstName == firstName)
            assert(userInRepo.lastName == lastName)
            assert(userInRepo.country?.id == countryId)
            assert(userInRepo.phoneNumber == phoneNumber)
            assert(userInRepo.authMethod == signupMethod)
            assert(userInRepo.role.id == UserRoleType.USER.id)
            assert(userInRepo.createdAt.isBefore(ZonedDateTime.now()))
            // TODO: decide how to test enabled properties
        }

        databaseCleanerService.deleteAll()
    }

    @Test
    fun incompleteSignupRequestShouldFail() {
        verify("The user cannot send malformed request to sign up") {
            val requestJson = """
            |{
                |"signup_method" : "EMAIL",
                |"user_info" : {
                    |"email" : "filipduj@gmail.com"
                |}
            |}""".trimMargin()

            mockMvc.perform(
                    post(pathSignup)
                            .content(requestJson)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest)
        }
    }

    @Test
    fun invalidDataSignupRequestShouldFail() {
        verify("The user cannot send request with invalid data (e.g. wrong mail format)") {
            val invalidJsonRequest = generateSignupJson(
                    email = "invalid-mail.com",
                    password = "unsafepassword123",
                    firstName = "",
                    lastName = "NoFirstName",
                    countryId = 999,
                    phoneNumber = "012abc345wrong"
            )

            mockMvc.perform(
                    post(pathSignup)
                            .content(invalidJsonRequest)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest)
        }
    }

    @Test
    fun signupShouldFailIfUserAlreadyExists() {
        val email = "john@smith.com"

        suppose("User with email $email exists in database") {
            createTestUsers(email)
        }

        verify("The user cannnot sign up with already existing email") {
            val requestJson = generateSignupJson(email = email)
            val result = mockMvc.perform(
                    post(pathSignup)
                            .content(requestJson)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest)
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andReturn()

            val response: ErrorResponse = objectMapper.readValue(result.response.contentAsString)
            assert(response.reason == ResourceAlreadyExistsException::class.java.canonicalName)
        }

        databaseCleanerService.deleteAll()
    }

    @Test
    fun signupUsingFacebookMethod() {
        val socialUser = SocialUser(
                email = "johnsmith@gmail.com",
                firstName = "John",
                lastName = "Smith",
                countryId = 1
        )
        val fbToken = "token"

        suppose("Social service is mocked to return Facebook user") {
            Mockito.`when`(socialService.getFacebookUserInfo(fbToken))
                    .thenReturn(socialUser)
        }

        verify("The user can sign up with Facebook account") {
            verifySocialSignUp(AuthMethod.FACEBOOK, fbToken, socialUser)
        }

        databaseCleanerService.deleteAll()
    }

    @Test
    fun signupUsingGoogleMethod() {
        val socialUser = SocialUser(
                email = "johnsmith@gmail.com",
                firstName = "John",
                lastName = "Smith",
                countryId = null
        )
        val googleToken = "token"

        suppose("Social service is mocked to return Google user") {
            Mockito.`when`(socialService.getGoogleUserInfo(googleToken))
                    .thenReturn(socialUser)
        }

        verify("The user can sign up with Google account") {
            verifySocialSignUp(AuthMethod.GOOGLE, googleToken, socialUser)
        }

        databaseCleanerService.deleteAll()
    }


    private fun createTestUsers(email: String, authMethod: AuthMethod = AuthMethod.EMAIL): User {
        val request = CreateUserServiceRequest(
                email = email,
                password = "Password175!",
                firstName = "John",
                lastName = "Smith",
                countryId = 1,
                phoneNumber = "0951234567",
                authMethod = authMethod
        )
        return userService.create(request)
    }

    private fun generateSignupJson(
            email: String = "john@smith.com",
            password: String = "Password157!",
            firstName: String = "John",
            lastName: String = "Smith",
            countryId: Int = 1,
            phoneNumber: String = "0951234567"): String {
        return """
            |{
            |  "signup_method" : "${AuthMethod.EMAIL}",
            |  "user_info" : {
            |       "email" : "$email",
            |       "password" : "$password",
            |       "first_name" : "$firstName",
            |       "last_name" : "$lastName",
            |       "country_id" : $countryId,
            |       "phone_number" : "$phoneNumber"
            |   }
            |}
        """.trimMargin()
    }

    private fun verifySocialSignUp(
            authMethod: AuthMethod, token: String, expectedSocialUser: SocialUser) {
        lateinit var result: MvcResult

        suppose("User has obtained token on frontend and sends signup request") {
            val request = """
            |{
            |  "signup_method" : "$authMethod",
            |  "user_info" : {
            |    "token" : "$token"
            |  }
            |}
            """.trimMargin()

            result = mockMvc.perform(
                    post(pathSignup)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andReturn()
        }

        verify("The controller returned valid user") {
            val userResponse: UserResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(userResponse.email).isEqualTo(expectedSocialUser.email)
        }

        verify("The user is stored in database") {
            val optionalUserInRepo = userService.find(expectedSocialUser.email)

            assertThat(optionalUserInRepo.isPresent).isTrue()
            val userInRepo = optionalUserInRepo.get()

            assert(userInRepo.email == expectedSocialUser.email)
            assert(userInRepo.firstName == expectedSocialUser.firstName)
            assert(userInRepo.lastName == expectedSocialUser.lastName)
            if (expectedSocialUser.countryId != null) {
                assert(expectedSocialUser.countryId == userInRepo.country?.id)
            }
            assert(userInRepo.role.id == UserRoleType.USER.id)
        }
    }

}
