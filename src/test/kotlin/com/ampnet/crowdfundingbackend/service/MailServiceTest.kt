package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.TestBase
import com.ampnet.crowdfundingbackend.config.ApplicationProperties
import com.ampnet.crowdfundingbackend.service.impl.MailServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.subethamail.wiser.Wiser

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [ApplicationProperties::class])
@EnableConfigurationProperties
@Import(JavaMailSenderImpl::class)
class MailServiceTest : TestBase() {

    @Autowired
    private lateinit var mailSender: JavaMailSenderImpl
    @Autowired
    private lateinit var applicationProperties: ApplicationProperties

    private lateinit var service: MailServiceImpl
    private lateinit var wiser: Wiser
    private var defaultMailPort: Int = 0
    private val testContext = TestContext()

    @BeforeEach
    fun init() {
        defaultMailPort = mailSender.port
        wiser = Wiser(0)
        wiser.start()
        mailSender.port = wiser.server.port
        service = MailServiceImpl(mailSender, applicationProperties)
    }

    @AfterEach
    fun tearDown() {
        wiser.stop()
        mailSender.port = defaultMailPort
    }

    @Test
    fun mustSetCorrectOrganizationInvitationMail() {
        suppose("Service send organizationInvitation mail") {
            service.sendOrganizationInvitationMail(
                    testContext.receiverMail, testContext.invitedBy, testContext.organizationName)
        }

        verify("The mail is sent to right receiver and has correct data") {
            val mailList = wiser.messages
            assertThat(mailList).hasSize(1)
            val mail = mailList.first()
            assertThat(mail.envelopeSender).isEqualTo(applicationProperties.mail.sender)
            assertThat(mail.envelopeReceiver).isEqualTo(testContext.receiverMail)
            assertThat(mail.mimeMessage.subject).isEqualTo(service.invitationMailSubject)

            val mailText = mail.mimeMessage.content.toString()
            assertThat(mailText).contains(testContext.invitedBy)
            assertThat(mailText).contains(testContext.organizationName)
            assertThat(mailText).contains(applicationProperties.mail.organizationInvitationsLink)
        }
    }

    private class TestContext {
        val receiverMail = "test@test.com"
        val token = "test-token"
        val invitedBy = "Test User"
        val organizationName = "Organization test"
    }
}
