package com.ampnet.crowdfundingbackend.blockchain

import org.web3j.crypto.RawTransaction
import org.web3j.protocol.core.methods.response.TransactionReceipt

interface BlockchainService {

    fun getProtocolVersion(): String

    fun postTransaction(tx: String): String

    fun contractCall(fromAddress: String, toAddress: String, encodedFunction: String): String

    // Think about how to modify these two in order to remove web3j dependencies (remain blockchain agnostic)
    fun generateContractCreateTx(bytecode: String, constructor: String, ownerAddress: String): RawTransaction
    fun getTransactionReceipt(txHash: String): TransactionReceipt
}
