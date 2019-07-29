package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.service.MailService
import com.ampnet.mailservice.proto.Empty
import com.ampnet.mailservice.proto.MailServiceGrpc
import com.ampnet.mailservice.proto.OrganizationInvitationRequest
import io.grpc.stub.StreamObserver
import mu.KLogging
import net.devh.boot.grpc.client.channelfactory.GrpcChannelFactory
import org.springframework.stereotype.Service

@Service
class MailServiceImpl(
    private val grpcChannelFactory: GrpcChannelFactory
) : MailService {

    companion object : KLogging()

    private val mailServiceStub: MailServiceGrpc.MailServiceStub by lazy {
        val channel = grpcChannelFactory.createChannel("mail-service")
        MailServiceGrpc.newStub(channel)
    }

    override fun sendOrganizationInvitationMail(to: String, organizationName: String) {
        val request = OrganizationInvitationRequest.newBuilder()
                .setTo(to)
                .setOrganization(organizationName)
                .build()

        mailServiceStub.sendOrganizationInvitation(request, object : StreamObserver<Empty> {
            override fun onNext(value: Empty?) {
                logger.debug { "Successfully sent organization invitation mail to: $to" }
            }

            override fun onError(t: Throwable?) {
                logger.warn { "Failed to sent organization invitation mail to: $to. ${t?.message}" }
            }

            override fun onCompleted() {
                // successfully sent invitation mail
            }
        })
    }
}
