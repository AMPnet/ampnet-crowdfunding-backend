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

    val confirmationMailSubject = "Confirmation mail"
    val invitationMailSubject = "Invitation to join organization"

    @Async
    override fun sendOrganizationInvitationMail(to: String, invitedBy: String, organizationName: String) {
        val message = "You have been invited by $invitedBy to join organization: $organizationName.\n" +
                "To review invite, please follow the link: ${applicationProperties.mail.organizationInvitationsLink}"
        val mail = createMailMessage(to, invitationMailSubject, message)
        if (applicationProperties.mail.enabled) {
            sendEmail(mail)
        } else {
            logger.warn { "Sending email is disabled. \nEmail: $mail" }
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
            logger.error(ex) { "Cannot send email to: ${mail.to}" }
        }
    }

    private fun getSenderMail(): String = applicationProperties.mail.sender
}
