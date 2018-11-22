package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.response.UserResponse
import com.ampnet.crowdfundingbackend.enums.UserRoleType
import com.ampnet.crowdfundingbackend.exception.ErrorResponse
import com.ampnet.crowdfundingbackend.exception.ResourceAlreadyExistsException
import com.ampnet.crowdfundingbackend.persistence.model.AuthMethod
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.persistence.repository.MailTokenDao
import com.ampnet.crowdfundingbackend.service.MailService
import com.ampnet.crowdfundingbackend.service.SocialService
import com.ampnet.crowdfundingbackend.service.UserService
import com.ampnet.crowdfundingbackend.service.pojo.CreateUserServiceRequest
import com.ampnet.crowdfundingbackend.service.pojo.SocialUser
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.ZonedDateTime

@ActiveProfiles("SocialMockConfig")
class RegistrationControllerTest : ControllerTestBase() {

    private val pathSignup = "/signup"

    @Autowired
    private lateinit var userService: UserService
    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder
    @Autowired
    private lateinit var socialService: SocialService
    @Autowired
    private lateinit var mailTokenDao: MailTokenDao
    @Autowired
    private lateinit var mailService: MailService

    private lateinit var testUser: TestUser
    private lateinit var testContext: TestContext

    @Before
    fun initTestData() {
        testUser = TestUser()
        testContext = TestContext()
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
                    .andExpect(MockMvcResultMatchers.status().isOk)
                    .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andReturn()
        }

        verify("The controller returned valid user") {
            val userResponse: UserResponse = objectMapper.readValue(testContext.mvcResult.response.contentAsString)
            Assertions.assertThat(userResponse.email).isEqualTo(testUser.email)
        }
        verify("The user is stored in database") {
            val userInRepo = userService.find(testUser.email)
            Assertions.assertThat(userInRepo).isNotNull

            assert(userInRepo!!.email == testUser.email)
            assert(passwordEncoder.matches(testUser.password, userInRepo.password))
            assert(userInRepo.firstName == testUser.firstName)
            assert(userInRepo.lastName == testUser.lastName)
            assert(userInRepo.country?.id == testUser.countryId)
            assert(userInRepo.phoneNumber == testUser.phoneNumber)
            assert(userInRepo.authMethod == testUser.authMethod)
            assert(userInRepo.role.id == UserRoleType.USER.id)
            assert(userInRepo.createdAt.isBefore(ZonedDateTime.now()))
            assert(userInRepo.enabled)
        }
        verify("The user confirmation token is created") {
            val userInRepo = userService.find(testUser.email)
            Assertions.assertThat(userInRepo).isNotNull
            val mailToken = mailTokenDao.findByUserId(userInRepo!!.id)
            assertThat(mailToken).isPresent
            assertThat(mailToken.get().token).isNotNull()
            assertThat(mailToken.get().createdAt).isBeforeOrEqualTo(ZonedDateTime.now())

            testContext.mailConfirmationToken = mailToken.get().token.toString()
        }
        verify("Sending mail was initiated") {
            Mockito.verify(mailService, Mockito.times(1))
                    .sendConfirmationMail(testUser.email, testContext.mailConfirmationToken)
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
                    .andExpect(MockMvcResultMatchers.status().isBadRequest)
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
                    .andExpect(MockMvcResultMatchers.status().isBadRequest)
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
                    .andExpect(MockMvcResultMatchers.status().isBadRequest)
                    .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
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
            Assertions.assertThat(userResponse.email).isEqualTo(expectedSocialUser.email)
        }

        verify("The user is stored in database") {
            val userInRepo = userService.find(expectedSocialUser.email)
            Assertions.assertThat(userInRepo).isNotNull

            assert(userInRepo!!.email == expectedSocialUser.email)
            assert(userInRepo.firstName == expectedSocialUser.firstName)
            assert(userInRepo.lastName == expectedSocialUser.lastName)
            if (expectedSocialUser.countryId != null) {
                assert(expectedSocialUser.countryId == userInRepo.country?.id)
            }
            assert(userInRepo.role.id == UserRoleType.USER.id)
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
        lateinit var mailConfirmationToken: String
    }
}
