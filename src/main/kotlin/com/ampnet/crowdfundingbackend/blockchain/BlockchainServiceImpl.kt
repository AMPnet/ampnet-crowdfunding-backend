package com.ampnet.crowdfundingbackend.blockchain

import com.ampnet.crowdfunding.proto.BalanceRequest
import com.ampnet.crowdfunding.proto.BlockchainServiceGrpc
import mu.KLogging
import net.devh.springboot.autoconfigure.grpc.client.GrpcChannelFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class BlockchainServiceImpl(
    @Qualifier("addressChannelFactory") grpcChannelFactory: GrpcChannelFactory
) : BlockchainService {

    companion object : KLogging()

    private val serviceBlockingStub: BlockchainServiceGrpc.BlockchainServiceBlockingStub by lazy {
        val channel = grpcChannelFactory.createChannel("blockchain-service")
        BlockchainServiceGrpc.newBlockingStub(channel)
    }

    override fun getBalance(address: String): Long {
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
