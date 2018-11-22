package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.config.ApplicationProperties
import com.ampnet.crowdfundingbackend.service.MailService
import mu.KLogging
import org.springframework.mail.MailException
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.Date

@Service
class MailServiceImpl(
    private val mailSender: JavaMailSender,
    private val applicationProperties: ApplicationProperties
) : MailService {

    companion object : KLogging()

    private val confirmationMail = "Confirmation mail"

    @Async
    override fun sendConfirmationMail(to: String, token: String) {
        val link = getConfirmationLink(token)
        val message = "Follow the link the confirm your email: $link"
        val mail = createMailMessage(to, confirmationMail, message)
        if (applicationProperties.mail.enabled) {
            sendEmail(mail)
        } else {
            logger.info { "Sending email is disabled. \nEmail: $mail" }
        }
    }

    private fun createMailMessage(to: String, subject: String, text: String): SimpleMailMessage {
        val mail = SimpleMailMessage()
        mail.setFrom(getSenderMail())
        mail.setSubject(subject)
        mail.setTo(to)
        mail.setText(text)
        mail.setSentDate(Date())
        return mail
    }

    private fun sendEmail(mail: SimpleMailMessage) {
        logger.info { "Sending mail: $mail " }
        try {
            mailSender.send(mail)
            logger.info { "Successfully sent email to: ${mail.to}" }
        } catch (ex: MailException) {
            logger.warn(ex) { "Cannot send email to: ${mail.to}" }
        }
    }

    private fun getSenderMail(): String = applicationProperties.mail.sender

    private fun getConfirmationLink(token: String): String =
            "${applicationProperties.mail.confirmationBaseLink}?token=$token"
}
