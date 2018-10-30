package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.config.DatabaseCleanerService
import com.ampnet.crowdfundingbackend.controller.pojo.request.UserUpdateRequest
import com.ampnet.crowdfundingbackend.controller.pojo.response.UserResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.UsersListResponse
import com.ampnet.crowdfundingbackend.enums.PrivilegeType
import com.ampnet.crowdfundingbackend.enums.UserRoleType
import com.ampnet.crowdfundingbackend.exception.ErrorResponse
import com.ampnet.crowdfundingbackend.exception.ResourceAlreadyExistsException
import com.ampnet.crowdfundingbackend.persistence.model.AuthMethod
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.security.WithMockCrowdfoundUser
import com.ampnet.crowdfundingbackend.service.SocialService
import com.ampnet.crowdfundingbackend.service.UserService
import com.ampnet.crowdfundingbackend.service.pojo.CreateUserServiceRequest
import com.ampnet.crowdfundingbackend.service.pojo.SocialUser
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
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
class UserControllerTest : ControllerTestBase() {

    private val pathUsers = "/users"
    private val pathSignup = "/signup"
    private val pathMe = "/me"

    private lateinit var testUser: TestUser
    private lateinit var testContext: TestContext

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var socialService: SocialService

    @Autowired
    private lateinit var databaseCleanerService: DatabaseCleanerService

    @Before
    fun initTestData() {
        testUser = TestUser()
        testContext = TestContext()
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
    fun mustBeAbleToSignUpUser() {
        suppose("The user send request to sign up") {
            databaseCleanerService.deleteAllUsers()
            val requestJson = generateSignupJson()
            testContext.mvcResult = mockMvc.perform(
                    post(pathSignup)
                            .content(requestJson)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk)
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andReturn()
        }

        verify("The controller returned valid user") {
            val userResponse: UserResponse = objectMapper.readValue(testContext.mvcResult.response.contentAsString)
            assertThat(userResponse.email).isEqualTo(testUser.email)
        }
        verify("The user is stored in database") {
            val userInRepo = userService.find(testUser.email)
            assertThat(userInRepo).isNotNull

            assert(userInRepo!!.email == testUser.email)
            assert(passwordEncoder.matches(testUser.password, userInRepo.password))
            assert(userInRepo.firstName == testUser.firstName)
            assert(userInRepo.lastName == testUser.lastName)
            assert(userInRepo.country?.id == testUser.countryId)
            assert(userInRepo.phoneNumber == testUser.phoneNumber)
            assert(userInRepo.authMethod == testUser.authMethod)
            assert(userInRepo.role.id == UserRoleType.USER.id)
            assert(userInRepo.createdAt.isBefore(ZonedDateTime.now()))
            // TODO: decide how to test enabled properties
        }
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
            testUser.email = "invalid-mail.com"
            testUser.password = "unsafepassword123"
            testUser.firstName = ""
            testUser.lastName = "NoFirstName"
            testUser.countryId = 999
            testUser.phoneNumber = "012abc345wrong"
            val invalidJsonRequest = generateSignupJson()

            mockMvc.perform(
                    post(pathSignup)
                            .content(invalidJsonRequest)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest)
        }
    }

    @Test
    fun signupShouldFailIfUserAlreadyExists() {
        suppose("User with email ${testUser.email} exists in database") {
            databaseCleanerService.deleteAllUsers()
            saveTestUser()
        }

        verify("The user cannnot sign up with already existing email") {
            val requestJson = generateSignupJson()
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
    }

    @Test
    fun signupUsingFacebookMethod() {
        suppose("Social service is mocked to return Facebook user") {
            databaseCleanerService.deleteAllUsers()
            testContext.socialUser = SocialUser(
                    email = "johnsmith@gmail.com",
                    firstName = "John",
                    lastName = "Smith",
                    countryId = 1
            )
            Mockito.`when`(socialService.getFacebookUserInfo(testContext.token))
                    .thenReturn(testContext.socialUser)
        }

        verify("The user can sign up with Facebook account") {
            verifySocialSignUp(AuthMethod.FACEBOOK, testContext.token, testContext.socialUser)
        }
    }

    @Test
    fun signupUsingGoogleMethod() {
        suppose("Social service is mocked to return Google user") {
            databaseCleanerService.deleteAllUsers()
            testContext.socialUser = SocialUser(
                    email = "johnsmith@gmail.com",
                    firstName = "John",
                    lastName = "Smith",
                    countryId = null
            )
            Mockito.`when`(socialService.getGoogleUserInfo(testContext.token))
                    .thenReturn(testContext.socialUser)
        }

        verify("The user can sign up with Google account") {
            verifySocialSignUp(AuthMethod.GOOGLE, testContext.token, testContext.socialUser)
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

    private fun generateSignupJson(): String {
        return """
            |{
            |  "signup_method" : "${testUser.authMethod}",
            |  "user_info" : {
            |       "email" : "${testUser.email}",
            |       "password" : "${testUser.password}",
            |       "first_name" : "${testUser.firstName}",
            |       "last_name" : "${testUser.lastName}",
            |       "country_id" : ${testUser.countryId},
            |       "phone_number" : "${testUser.phoneNumber}"
            |   }
            |}
        """.trimMargin()
    }

    private fun verifySocialSignUp(authMethod: AuthMethod, token: String, expectedSocialUser: SocialUser) {
        suppose("User has obtained token on frontend and sends signup request") {
            val request = """
            |{
            |  "signup_method" : "$authMethod",
            |  "user_info" : {
            |    "token" : "$token"
            |  }
            |}
            """.trimMargin()

            testContext.mvcResult = mockMvc.perform(
                    post(pathSignup)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andReturn()
        }

        verify("The controller returned valid user") {
            val userResponse: UserResponse = objectMapper.readValue(testContext.mvcResult.response.contentAsString)
            assertThat(userResponse.email).isEqualTo(expectedSocialUser.email)
        }

        verify("The user is stored in database") {
            val userInRepo = userService.find(expectedSocialUser.email)
            assertThat(userInRepo).isNotNull

            assert(userInRepo!!.email == expectedSocialUser.email)
            assert(userInRepo.firstName == expectedSocialUser.firstName)
            assert(userInRepo.lastName == expectedSocialUser.lastName)
            if (expectedSocialUser.countryId != null) {
                assert(expectedSocialUser.countryId == userInRepo.country?.id)
            }
            assert(userInRepo.role.id == UserRoleType.USER.id)
        }
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

    private class TestContext {
        lateinit var mvcResult: MvcResult
        lateinit var socialUser: SocialUser
        val token = "token"
    }
}
