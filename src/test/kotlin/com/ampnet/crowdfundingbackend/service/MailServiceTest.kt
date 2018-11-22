package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.TestBase
import com.ampnet.crowdfundingbackend.config.ApplicationProperties
import com.ampnet.crowdfundingbackend.service.impl.MailServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.test.context.junit4.SpringRunner
import org.subethamail.wiser.Wiser

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [ApplicationProperties::class])
@EnableConfigurationProperties
@Import(JavaMailSenderImpl::class)
class MailServiceTest : TestBase() {

    @Autowired
    private lateinit var mailSender: JavaMailSenderImpl
    @Autowired
    private lateinit var applicationProperties: ApplicationProperties

    private lateinit var service: MailService
    private lateinit var wiser: Wiser
    private var defaultMailPort: Int = 0

    private val receiverMail = "test@test.com"
    private val token = "test-token"

    @Before
    fun init() {
        defaultMailPort = mailSender.port
        wiser = Wiser(0)
        wiser.start()
        mailSender.port = wiser.server.port
        service = MailServiceImpl(mailSender, applicationProperties)
    }

    @After
    fun tearDown() {
        wiser.stop()
        mailSender.port = defaultMailPort
    }

    @Test
    fun mustSetCorrectSenderMailFromProperties() {
        suppose("Service sent the mail") {
            service.sendConfirmationMail(receiverMail, token)
        }

        verify("The mail is sent to right receiver and has confirmation link") {
            val mailList = wiser.messages
            assertThat(mailList).hasSize(1)
            val mail = mailList.first()
            assertThat(mail.envelopeSender).isEqualTo(applicationProperties.mail.sender)
            assertThat(mail.envelopeReceiver).isEqualTo(receiverMail)
            assertThat(mail.mimeMessage.subject).isEqualTo("Confirmation mail")

            val confirmationLink = "${applicationProperties.mail.confirmationBaseLink}?token=$token"
            assertThat(mail.mimeMessage.content.toString()).contains(confirmationLink)
        }
    }
}
