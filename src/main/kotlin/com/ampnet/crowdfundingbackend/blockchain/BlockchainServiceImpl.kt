package com.ampnet.crowdfundingbackend.blockchain

import com.ampnet.crowdfunding.proto.ActivateOrganizationRequest
import com.ampnet.crowdfunding.proto.AddWalletRequest
import com.ampnet.crowdfunding.proto.BalanceRequest
import com.ampnet.crowdfunding.proto.BlockchainServiceGrpc
import com.ampnet.crowdfunding.proto.GenerateAddOrganizationTxRequest
import com.ampnet.crowdfunding.proto.GenerateAddProjectTxRequest
import com.ampnet.crowdfunding.proto.PostVaultTxRequest
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.InternalException
import com.ampnet.crowdfundingbackend.service.pojo.GenerateProjectWalletRequest
import com.ampnet.crowdfundingbackend.service.pojo.PostTransactionType
import com.ampnet.crowdfundingbackend.service.pojo.TransactionData
import io.grpc.StatusRuntimeException
import mu.KLogging
import net.devh.boot.grpc.client.channelfactory.GrpcChannelFactory
import org.springframework.stereotype.Service

@Service
class BlockchainServiceImpl(
    private val grpcChannelFactory: GrpcChannelFactory
) : BlockchainService {

    companion object : KLogging()

    private val serviceBlockingStub: BlockchainServiceGrpc.BlockchainServiceBlockingStub by lazy {
        val channel = grpcChannelFactory.createChannel("blockchain-service")
        BlockchainServiceGrpc.newBlockingStub(channel)
    }

    override fun getBalance(hash: String): Long? {
        logger.info { "Fetching balance for hash: $hash" }
        return try {
            val response = serviceBlockingStub.getBalance(
                    BalanceRequest.newBuilder()
                            .setWalletTxHash(hash)
                            .build()
            )
            logger.info { "Received response: $response" }
            response.balance
        } catch (ex: StatusRuntimeException) {
            logger.error(ex) { "Could not get balance for wallet: $hash" }
            null
        }
    }

    override fun addWallet(address: String, publicKey: String): String? {
        logger.info { "Adding wallet: $address" }
        return try {
            val response = serviceBlockingStub.addWallet(
                    AddWalletRequest.newBuilder()
                            .setAddress(address)
                            .setPublicKey(publicKey)
                            .build()
            )
            logger.info { "Successfully added wallet: $response" }
            return response.txHash
        } catch (ex: StatusRuntimeException) {
            logger.error(ex) { "Could not add wallet: $address" }
            null
        }
    }

    override fun generateAddOrganizationTransaction(userWalletHash: String, name: String): TransactionData {
        logger.info { "Generating add organization: $name" }
        try {
            val response = serviceBlockingStub.generateAddOrganizationTx(
                    GenerateAddOrganizationTxRequest.newBuilder()
                            .setFromTxHash(userWalletHash)
                            .build()
            )
            return TransactionData(response)
        } catch (ex: StatusRuntimeException) {
            logger.error(ex) { "Could not generate Add Organization transaction" }
            throw InternalException(ErrorCode.INT_ORG, "Failed to create Organization on blockchain")
        }
    }

    override fun generateProjectWalletTransaction(request: GenerateProjectWalletRequest): TransactionData {
        logger.info { "Generating Project wallet transaction" }
        try {
            val response = serviceBlockingStub.generateAddOrganizationProjectTx(
                    GenerateAddProjectTxRequest.newBuilder()
                            .setFromTxHash(request.userWalletHash)
                            .setOrganizationTxHash(request.organizationHash)
                            .setMaxInvestmentPerUser(request.maxPerUser)
                            .setMinInvestmentPerUser(request.minPerUser)
                            .setInvestmentCap(request.investmentCap)
                            .build()
            )
            return TransactionData(response)
        } catch (ex: StatusRuntimeException) {
            logger.error(ex) { "Could not generate Project wallet transaction: $request" }
            throw InternalException(ErrorCode.INT_WALLET_ADD, "Could not create wallet for project")
        }
    }

    override fun postTransaction(transaction: String, type: PostTransactionType): String {
        try {
            val response = serviceBlockingStub.postVaultTransaction(
                    PostVaultTxRequest.newBuilder()
                            .setData(transaction)
                            .setTxType(type.type)
                            .build()
            )
            return response.txHash
        } catch (ex: StatusRuntimeException) {
            logger.error(ex) { "Could not post type: $type transaction: $transaction" }
            throw InternalException(ErrorCode.INT_TRANSACTION, "Could not post transaction")
        }
    }

    override fun activateOrganization(organizationWalletHash: String): String {
        logger.info { "Activating organization with wallet hash: $organizationWalletHash" }
        try {
            val response = serviceBlockingStub.activateOrganization(
                    ActivateOrganizationRequest.newBuilder()
                            .setOrganizationTxHash(organizationWalletHash)
                            .build()
            )
            return response.txHash
        } catch (ex: StatusRuntimeException) {
            logger.error(ex) { "Could not activate organization: $organizationWalletHash" }
            throw InternalException(ErrorCode.INT_ORG_ACTIVATE,
                    "Could not activate organization: $organizationWalletHash")
        }
    }
}
