package com.ampnet.crowdfundingbackend.blockchain.impl

import com.ampnet.crowdfundingbackend.blockchain.BlockchainService
import com.ampnet.crowdfundingbackend.config.ApplicationProperties
import org.springframework.stereotype.Service
import org.web3j.crypto.RawTransaction
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.protocol.http.HttpService
import java.math.BigInteger

@Service
class BlockchainServiceImpl(applicationProperties: ApplicationProperties) : BlockchainService {

    private val web3j: Web3j by lazy {
        Web3j.build(HttpService(applicationProperties.web3j.clientAddress))
    }

    override fun getProtocolVersion(): String {
        return web3j.ethProtocolVersion().send().protocolVersion
    }

    override fun generateContractCreateTx(bytecode: String, constructor: String, ownerAddress: String): RawTransaction {
        val gasPrice = web3j.ethGasPrice().send().gasPrice

        // How to set gas limit properly? It should be just a little bit above required value
        val gasLimit = BigInteger.valueOf(500000)

        val nonce = web3j.ethGetTransactionCount(
                ownerAddress,
                DefaultBlockParameterName.LATEST
        ).send().transactionCount

        return RawTransaction.createContractTransaction(
                nonce,
                gasPrice,
                gasLimit,
                BigInteger.ZERO,
                bytecode + constructor
        )
    }

    override fun postTransaction(tx: String): String {
        return web3j.ethSendRawTransaction(tx).send().transactionHash
    }

    override fun getTransactionReceipt(txHash: String): TransactionReceipt {
        return web3j.ethGetTransactionReceipt(txHash).send().transactionReceipt.get()
    }

    override fun contractCall(fromAddress: String, toAddress: String, encodedFunction: String): String {
        val transaction = org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                fromAddress,
                toAddress,
                encodedFunction
        )

        return web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).send().value
    }
}
