package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfunding.BalanceRequest
import com.ampnet.crowdfunding.TestServiceGrpc
import com.ampnet.crowdfundingbackend.service.BlockchainService
import mu.KLogging
import net.devh.springboot.autoconfigure.grpc.client.GrpcChannelFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class BlockchainServiceImpl(
    @Qualifier("addressChannelFactory") grpcChannelFactory: GrpcChannelFactory
) : BlockchainService {

    companion object : KLogging()

    private val serviceBlockingStub: TestServiceGrpc.TestServiceBlockingStub by lazy {
        val channel = grpcChannelFactory.createChannel("blockchain-service")
        TestServiceGrpc.newBlockingStub(channel)
    }

    override fun getBalance(address: String): String {
        logger.debug { "Fetching balance for address: $address" }
        val response = serviceBlockingStub.getBalance(
                BalanceRequest.newBuilder()
                        .setAddress(address)
                        .build()
        )
        logger.debug { "Received response: $response" }
        return response.balance
    }
}
