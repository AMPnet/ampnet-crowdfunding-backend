package com.ampnet.crowdfundingbackend.blockchain

import com.ampnet.crowdfunding.proto.BalanceRequest
import com.ampnet.crowdfunding.proto.BlockchainServiceGrpc
import com.ampnet.crowdfundingbackend.config.ApplicationProperties
import io.grpc.StatusRuntimeException
import mu.KLogging
import net.devh.boot.grpc.client.channelfactory.GrpcChannelFactory
import org.springframework.stereotype.Service

@Service
class BlockchainServiceImpl(
    private val applicationProperties: ApplicationProperties,
    private val grpcChannelFactory: GrpcChannelFactory
) : BlockchainService {

    companion object : KLogging()

    private val serviceBlockingStub: BlockchainServiceGrpc.BlockchainServiceBlockingStub by lazy {
        val channel = grpcChannelFactory.createChannel("blockchain-service")
        BlockchainServiceGrpc.newBlockingStub(channel)
    }

    override fun getBalance(address: String): Long? {
        logger.info { "Fetching balance for address: $address" }
        return try {
            val response = serviceBlockingStub.getBalance(
                    BalanceRequest.newBuilder()
                            .setAddress(address)
                            .build()
            )
            logger.info { "Received response: $response" }
            response.balance
        } catch (ex: StatusRuntimeException) {
            logger.error(ex) { "Could not get balance for wallet: $address" }
            null
        }
    }
}
