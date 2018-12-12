package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.response.UserResponse
import com.ampnet.crowdfundingbackend.enums.UserRoleType
import com.ampnet.crowdfundingbackend.exception.ErrorResponse
import com.ampnet.crowdfundingbackend.enums.AuthMethod
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.persistence.repository.MailTokenRepository
import com.ampnet.crowdfundingbackend.security.WithMockCrowdfoundUser
import com.ampnet.crowdfundingbackend.service.MailService
import com.ampnet.crowdfundingbackend.service.SocialService
import com.ampnet.crowdfundingbackend.service.UserService
import com.ampnet.crowdfundingbackend.service.pojo.CreateUserServiceRequest
import com.ampnet.crowdfundingbackend.service.pojo.SocialUser
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.ZonedDateTime
import java.util.UUID

@ActiveProfiles("SocialMockConfig")
class RegistrationControllerTest : ControllerTestBase() {

    private val pathSignup = "/signup"
    private val confirmationPath = "/mail-confirmation"
    private val resendConfirmationPath = "/mail-confirmation/resend"

    @Autowired
    private lateinit var userService: UserService
    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder
    @Autowired
    private lateinit var socialService: SocialService
    @Autowired
    private lateinit var mailTokenRepository: MailTokenRepository
    @Autowired
    private lateinit var mailService: MailService

    private lateinit var testUser: TestUser
    private lateinit var testContext: TestContext

    @BeforeEach
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
            assertThat(userInRepo.enabled).isFalse()
        }
        verify("The user confirmation token is created") {
            val userInRepo = userService.find(testUser.email)
            Assertions.assertThat(userInRepo).isNotNull
            val mailToken = mailTokenRepository.findByUserId(userInRepo!!.id)
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
    fun invalidCountryIdSignupRequestShouldFail() {
        verify("The user cannot send request with invalid country id") {
            testUser.email = "invalid@mail.com"
            testUser.password = "passsssword"
            testUser.firstName = "Name"
            testUser.lastName = "NoFirstName"
            testUser.countryId = 0
            testUser.phoneNumber = "0981234567"
            val invalidJsonRequest = generateSignupJson()

            val result = mockMvc.perform(
                    post(pathSignup)
                            .content(invalidJsonRequest)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest)
                    .andReturn()

            verifyResponseErrorCode(result, ErrorCode.REG_INVALID)
        }
    }

    @Test
    fun emptyNameSignupRequestShouldFail() {
        verify("The user cannot send request with empty name") {
            testUser.email = "test@email.com"
            testUser.password = "passsssword"
            testUser.firstName = ""
            testUser.lastName = "NoFirstName"
            testUser.countryId = 1
            testUser.phoneNumber = "0981234567"
            val invalidJsonRequest = generateSignupJson()

            val result = mockMvc.perform(
                    post(pathSignup)
                            .content(invalidJsonRequest)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest)
                    .andReturn()

            verifyResponseErrorCode(result, ErrorCode.REG_INVALID)
        }
    }

    @Test
    fun invalidEmailSignupRequestShouldFail() {
        verify("The user cannot send request with invalid email") {
            testUser.email = "invalid-mail.com"
            testUser.password = "passssword"
            testUser.firstName = "Name"
            testUser.lastName = "NoFirstName"
            testUser.countryId = 1
            testUser.phoneNumber = "0981234567"
            val invalidJsonRequest = generateSignupJson()

            val result = mockMvc.perform(
                    post(pathSignup)
                            .content(invalidJsonRequest)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest)
                    .andReturn()

            verifyResponseErrorCode(result, ErrorCode.REG_INVALID)
        }
    }

    @Test
    fun shortPasswordSignupRequestShouldFail() {
        verify("The user cannot send request with too short passowrd") {
            testUser.email = "invalid@mail.com"
            testUser.password = "short"
            testUser.firstName = "Name"
            testUser.lastName = "NoFirstName"
            testUser.countryId = 1
            testUser.phoneNumber = "0981234567"
            val invalidJsonRequest = generateSignupJson()

            val result = mockMvc.perform(
                    post(pathSignup)
                            .content(invalidJsonRequest)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest)
                    .andReturn()

            verifyResponseErrorCode(result, ErrorCode.REG_INVALID)
        }
    }

    @Test
    fun invalidPhoneNumberSignupRequestShouldFail() {
        verify("The user cannot send request with invalid phone number") {
            testUser.email = "invalid@mail.com"
            testUser.password = "passssword"
            testUser.firstName = "Name"
            testUser.lastName = "NoFirstName"
            testUser.countryId = 1
            testUser.phoneNumber = "012abc345wrong"
            val invalidJsonRequest = generateSignupJson()

            val result = mockMvc.perform(
                    post(pathSignup)
                            .content(invalidJsonRequest)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest)
                    .andReturn()

            verifyResponseErrorCode(result, ErrorCode.REG_INVALID)
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
            val expectedErrorCode = getResponseErrorCode(ErrorCode.REG_USER_EXISTS)
            assert(response.errCode == expectedErrorCode)
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
    fun mustBeAbleToConfirmEmail() {
        suppose("The user is created with unconfirmed email") {
            createUnconfirmedUser()
        }

        verify("The user can confirm email with mail token") {
            val mailToken = mailTokenRepository.findByUserId(testUser.id)
            assertThat(mailToken).isPresent

            mockMvc.perform(get("$confirmationPath?token=${mailToken.get().token}"))
                    .andExpect(status().isOk)
        }
        verify("The user is confirmed in database") {
            val user = userService.find(testUser.id)
            assertThat(user).isNotNull
            assertThat(user!!.enabled).isTrue()
        }
    }

    @Test
    fun mustGetBadRequestForInvalidTokenFormat() {
        verify("Invalid token format will get bad response") {
            mockMvc.perform(get("$confirmationPath?token=bezvezni-token-tak"))
                    .andExpect(status().isBadRequest)
        }
    }

    @Test
    fun mustGetNotFoundRandomToken() {
        verify("Random token will get not found response") {
            val randomToken = UUID.randomUUID().toString()
            mockMvc.perform(get("$confirmationPath?token=$randomToken"))
                    .andExpect(status().isNotFound)
        }
    }

    @Test
    fun mustNotBeAbleToConfirmEmailWithExpiredToken() {
        suppose("The user is created with unconfirmed email") {
            createUnconfirmedUser()
        }
        suppose("The token has expired") {
            val optionalMailToken = mailTokenRepository.findByUserId(testUser.id)
            assertThat(optionalMailToken).isPresent
            val mailToken = optionalMailToken.get()
            mailToken.createdAt = ZonedDateTime.now().minusDays(2)
            mailTokenRepository.save(mailToken)
        }

        verify("The user cannot confirm email with expired token") {
            val optionalMailToken = mailTokenRepository.findByUserId(testUser.id)
            assertThat(optionalMailToken).isPresent
            mockMvc.perform(get("$confirmationPath?token=${optionalMailToken.get().token}"))
                    .andExpect(status().isBadRequest)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToResendConfirmationEmail() {
        suppose("The user has confirmation mail token") {
            testUser.email = defaultEmail
            createUnconfirmedUser()
            val optionalMailToken = mailTokenRepository.findByUserId(testUser.id)
            assertThat(optionalMailToken).isPresent
        }

        verify("User can request resend mail confirmation") {
            mockMvc.perform(get(resendConfirmationPath))
                    .andExpect(status().isOk)
        }
        verify("The user confirmation token is created") {
            val userInRepo = userService.find(testUser.email)
            assertThat(userInRepo).isNotNull
            val mailToken = mailTokenRepository.findByUserId(userInRepo!!.id)
            assertThat(mailToken).isPresent
            assertThat(mailToken.get().token).isNotNull()
            assertThat(mailToken.get().createdAt).isBeforeOrEqualTo(ZonedDateTime.now())

            testContext.mailConfirmationToken = mailToken.get().token.toString()
        }
        verify("Sending mail was initiated") {
            Mockito.verify(mailService, Mockito.times(1))
                    .sendConfirmationMail(testUser.email, testContext.mailConfirmationToken)
        }
        verify("The user can confirm mail with new token") {
            val mailToken = mailTokenRepository.findByUserId(testUser.id)
            assertThat(mailToken).isPresent

            mockMvc.perform(get("$confirmationPath?token=${mailToken.get().token}"))
                    .andExpect(status().isOk)
        }
        verify("The user is confirmed in database") {
            val userInRepo = userService.find(testUser.email)
            assertThat(userInRepo).isNotNull
            assertThat(userInRepo!!.enabled).isTrue()
        }
    }

    @Test
    fun unauthorizedUserCannotResendConfirmationEmail() {
        verify("User will get error unauthorized") {
            mockMvc.perform(get(resendConfirmationPath)).andExpect(status().isUnauthorized)
        }
    }

    private fun createUnconfirmedUser() {
        databaseCleanerService.deleteAllUsers()
        saveTestUser()
        val user = userService.find(testUser.id)
        assertThat(user).isNotNull
        assertThat(user!!.enabled).isFalse()
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
            assertThat(userInRepo.enabled).isTrue()
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
        val savedUser = userService.create(request)
        testUser.id = savedUser.id
        return savedUser
    }

    private class TestUser {
        var id = -1
        var email = "john@smith.com"
        var password = "abcdefgh"
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
