package com.ampnet.crowdfundingbackend.blockchain

import com.ampnet.crowdfunding.proto.ActivateOrganizationRequest
import com.ampnet.crowdfunding.proto.AddWalletRequest
import com.ampnet.crowdfunding.proto.BalanceRequest
import com.ampnet.crowdfunding.proto.BlockchainServiceGrpc
import com.ampnet.crowdfunding.proto.GenerateAddOrganizationTxRequest
import com.ampnet.crowdfunding.proto.GenerateAddProjectTxRequest
import com.ampnet.crowdfunding.proto.GenerateApproveWithdrawTxRequest
import com.ampnet.crowdfunding.proto.GenerateBurnFromTxRequest
import com.ampnet.crowdfunding.proto.GenerateConfirmInvestmentTxRequest
import com.ampnet.crowdfunding.proto.GenerateInvestmentTxRequest
import com.ampnet.crowdfunding.proto.GenerateMintTxRequest
import com.ampnet.crowdfunding.proto.PostVaultTxRequest
import com.ampnet.crowdfundingbackend.blockchain.pojo.ProjectInvestmentTxRequest
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.InternalException
import com.ampnet.crowdfundingbackend.blockchain.pojo.GenerateProjectWalletRequest
import com.ampnet.crowdfundingbackend.service.pojo.PostTransactionType
import com.ampnet.crowdfundingbackend.blockchain.pojo.TransactionData
import io.grpc.Status
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

    override fun getBalance(hash: String): Long {
        logger.info { "Fetching balance for hash: $hash" }
        try {
            val response = serviceBlockingStub.getBalance(
                    BalanceRequest.newBuilder()
                            .setWalletTxHash(hash)
                            .build()
            )
            logger.info { "Received response: $response" }
            return response.balance
        } catch (ex: StatusRuntimeException) {
            throw getInternalExceptionFromStatusException(ex, "Could not get balance for wallet: $hash")
        }
    }

    override fun addWallet(address: String, publicKey: String): String {
        logger.info { "Adding wallet: $address" }
        try {
            val response = serviceBlockingStub.addWallet(
                    AddWalletRequest.newBuilder()
                            .setAddress(address)
                            .setPublicKey(publicKey)
                            .build()
            )
            logger.info { "Successfully added wallet: $response" }
            return response.txHash
        } catch (ex: StatusRuntimeException) {
            throw getInternalExceptionFromStatusException(ex, "Could not add wallet: $address")
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
            throw getInternalExceptionFromStatusException(ex, "Could not generate transaction Add Organization: $name")
        }
    }

    override fun generateProjectWalletTransaction(request: GenerateProjectWalletRequest): TransactionData {
        logger.info { "Generating Project wallet transaction" }
        try {
            val endTimeInSeconds = request.endDate.toEpochSecond()
            val response = serviceBlockingStub.generateAddOrganizationProjectTx(
                    GenerateAddProjectTxRequest.newBuilder()
                            .setFromTxHash(request.userWalletHash)
                            .setOrganizationTxHash(request.organizationHash)
                            .setMaxInvestmentPerUser(request.maxPerUser)
                            .setMinInvestmentPerUser(request.minPerUser)
                            .setInvestmentCap(request.investmentCap)
                            .setEndInvestmentTime(endTimeInSeconds)
                            .build()
            )
            return TransactionData(response)
        } catch (ex: StatusRuntimeException) {
            throw getInternalExceptionFromStatusException(ex, "Could not generate Project wallet transaction: $request")
        }
    }

    override fun postTransaction(transaction: String, type: PostTransactionType): String {
        logger.info { "Post transaction type: $type" }
        try {
            val response = serviceBlockingStub.postVaultTransaction(
                    PostVaultTxRequest.newBuilder()
                            .setData(transaction)
                            .setTxType(type.type)
                            .build()
            )
            return response.txHash
        } catch (ex: StatusRuntimeException) {
            throw getInternalExceptionFromStatusException(ex, "Could not post type: $type transaction: $transaction")
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
            throw getInternalExceptionFromStatusException(
                ex, "Could not activate organization: $organizationWalletHash")
        }
    }

    override fun generateProjectInvestmentTransaction(request: ProjectInvestmentTxRequest): TransactionData {
        logger.info { "User: ${request.userWalletHash} is investing to project: ${request.projectWalletHash} " +
            "with amount ${request.amount}" }
        try {
            val response = serviceBlockingStub.generateInvestmentTx(
                GenerateInvestmentTxRequest.newBuilder()
                    .setFromTxHash(request.userWalletHash)
                    .setProjectTxHash(request.projectWalletHash)
                    .setAmount(request.amount)
                    .build()
            )
            return TransactionData(response)
        } catch (ex: StatusRuntimeException) {
            throw getInternalExceptionFromStatusException(
                ex, "Could not invest in project: ${request.projectWalletHash}")
        }
    }

    override fun generateConfirmInvestment(userWalletHash: String, projectWalletHash: String): TransactionData {
        logger.info { "Confirm user: $userWalletHash investment to project: $projectWalletHash" }
        try {
            val response = serviceBlockingStub.generateConfirmInvestmentTx(
                GenerateConfirmInvestmentTxRequest.newBuilder()
                    .setFromTxHash(userWalletHash)
                    .setProjectTxHash(projectWalletHash)
                    .build()
            )
            return TransactionData(response)
        } catch (ex: StatusRuntimeException) {
            throw getInternalExceptionFromStatusException(
                ex, "Could not confirm investment in project: $projectWalletHash")
        }
    }

    override fun generateMintTransaction(from: String, toHash: String, amount: Long): TransactionData {
        logger.warn { "Generating Mint transaction from: $from toHash: $toHash with amount = $amount" }
        try {
            val response = serviceBlockingStub.generateMintTx(
                GenerateMintTxRequest.newBuilder()
                    .setFrom(from)
                    .setToTxHash(toHash)
                    .setAmount(amount)
                    .build()
            )
            return TransactionData(response)
        } catch (ex: StatusRuntimeException) {
            throw getInternalExceptionFromStatusException(ex, "Could not Mint toHash: $toHash")
        }
    }

    override fun generateBurnTransaction(from: String, burnFromTxHash: String, amount: Long): TransactionData {
        logger.warn { "Generating Burn transaction from: $from burnFromTxHash: $burnFromTxHash with amount = $amount" }
        try {
            val response = serviceBlockingStub.generateBurnFromTx(
                GenerateBurnFromTxRequest.newBuilder()
                    .setFrom(from)
                    .setBurnFromTxHash(burnFromTxHash)
                    .setAmount(amount)
                    .build()
            )
            return TransactionData(response)
        } catch (ex: StatusRuntimeException) {
            throw getInternalExceptionFromStatusException(ex, "Could not Burn toHash: $burnFromTxHash")
        }
    }

    override fun generateApproveBurnTransaction(burnFromTxHash: String, amount: Long): TransactionData {
        logger.info { "Generating Approve Burn Transaction burnFromTxHash: $burnFromTxHash with amount = $amount" }
        try {
            val response = serviceBlockingStub.generateApproveWithdrawTx(
                GenerateApproveWithdrawTxRequest.newBuilder()
                        .setFromTxHash(burnFromTxHash)
                        .setAmount(amount)
                        .build()
            )
            return TransactionData(response)
        } catch (ex: StatusRuntimeException) {
            throw getInternalExceptionFromStatusException(ex, "Could not Burn toHash: $burnFromTxHash")
        }
    }

    private fun getInternalExceptionFromStatusException(
        ex: StatusRuntimeException,
        message: String
    ): InternalException {
        val grpcErrorCode = getErrorDescriptionFromExceptionStatus(ex.status)
        val errorCode = ErrorCode.INT_GRPC
        errorCode.specificCode = grpcErrorCode.code
        errorCode.message = grpcErrorCode.message
        return InternalException(errorCode, message)
    }

    // Status defined in ampenet-blockchain service, for more info see:
    // ampnet-blockchain-service/src/main/kotlin/com/ampnet/crowdfunding/blockchain/enums/ErrorCode.kt
    private fun getErrorDescriptionFromExceptionStatus(status: Status): GrpcErrorCode {
        val description = status.description?.split(" > ")
            ?: return GrpcErrorCode("90", "Could not parse error: ${status.description}")
        if (description.size != 2) {
            return GrpcErrorCode("91", "Wrong size of error message: $description")
        }
        return GrpcErrorCode(description[0], description[1])
    }

    private data class GrpcErrorCode(val code: String, val message: String)
}
