package com.ampnet.crowdfundingbackend.blockchain

import com.ampnet.crowdfundingbackend.TestBase
import com.ampnet.crowdfundingbackend.blockchain.impl.BlockchainServiceImpl
import com.ampnet.crowdfundingbackend.config.ApplicationProperties
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Utf8String
import org.web3j.crypto.TransactionEncoder
import org.web3j.crypto.WalletUtils
import org.web3j.utils.Numeric

@Disabled // remove for testing
@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [ApplicationProperties::class])
@EnableConfigurationProperties
class BlockchainServiceTest : TestBase() {

    @Autowired
    private lateinit var applicationProperties: ApplicationProperties

    private val blockchainService: BlockchainService by lazy {
        BlockchainServiceImpl(applicationProperties)
    }

    private val keystorePath = "src/test/resources/ethereum/keystore"

    /*
        private key: 0xa88400ec75febb4244f4a04d8290ae2fbdbedb874553eb86b91f10c9de4f5fa8
        address: 0x62368bb127fdae49cc15652fafb8317bd85a06c8
     */
    private val accountA = WalletUtils.loadCredentials(
            "password123",
            "$keystorePath/accountA"
    )

    /*
        private key: 0xec3cd0b40d2952cc77cac778461e89dd958684b43320ff0ba1cf3ee435badf32
        address: 0xe4103cf325090d04a3bcd807c3e860f0faad961e
     */
    private val accountB = WalletUtils.loadCredentials(
            "password123",
            "$keystorePath/accountB"
    )

    /*
        private key: 0x16675095b2ebbe3402d71c018158a8cef7b8cdad650e716de17c487190133932
        address: 0xe53532bba30cc4936768b0e12c57553bc7e5bc4c
     */
    private val accountC = WalletUtils.loadCredentials(
            "password123",
            "$keystorePath/accountC"
    )

    private val simpleContractSource = """
        pragma solidity ^0.4.16;

        contract SimpleContract {

            string text;

            constructor(string _text) public {
                text = _text;
            }

            function set(string _text) public {
                text = _text;
            }

            function get() public view returns (string) {
                return text;
            }

        }
    """.trimIndent()

    private val simpleContractBytecode = "0x608060405234801561001057600080fd5b506040516103db3803806103db833981018060405281019080805182019291905050508060009080519060200190610049929190610050565b50506100f5565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f1061009157805160ff19168380011785556100bf565b828001600101855582156100bf579182015b828111156100be5782518255916020019190600101906100a3565b5b5090506100cc91906100d0565b5090565b6100f291905b808211156100ee5760008160009055506001016100d6565b5090565b90565b6102d7806101046000396000f30060806040526004361061004c576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff1680634ed3885e146100515780636d4ce63c146100ba575b600080fd5b34801561005d57600080fd5b506100b8600480360381019080803590602001908201803590602001908080601f016020809104026020016040519081016040528093929190818152602001838380828437820191505050505050919291929050505061014a565b005b3480156100c657600080fd5b506100cf610164565b6040518080602001828103825283818151815260200191508051906020019080838360005b8381101561010f5780820151818401526020810190506100f4565b50505050905090810190601f16801561013c5780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b8060009080519060200190610160929190610206565b5050565b606060008054600181600116156101000203166002900480601f0160208091040260200160405190810160405280929190818152602001828054600181600116156101000203166002900480156101fc5780601f106101d1576101008083540402835291602001916101fc565b820191906000526020600020905b8154815290600101906020018083116101df57829003601f168201915b5050505050905090565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f1061024757805160ff1916838001178555610275565b82800160010185558215610275579182015b82811115610274578251825591602001919060010190610259565b5b5090506102829190610286565b5090565b6102a891905b808211156102a457600081600090555060010161028c565b5090565b905600a165627a7a7230582020fe749746e26e26af8fa034b4986bb09b864ff6dc9b62e759473942c94f1db50029"

    private lateinit var simpleContractDeployTxHash: String
    private val storedMessage = "Test Message"

    @Test
    fun basicNodeTest() {
        verify("Blockchain node defined in initialize-ganache-cli.sh is running.") {
            assert(blockchainService.getProtocolVersion().isNotEmpty())
        }
    }

    @Test
    fun simpleContractTest() {
        suppose("SimpleContract is deployed with custom message in constructor and accountA as owner.") {
            val encodedConstructor = FunctionEncoder.encodeConstructor(listOf(Utf8String(storedMessage)))
            val tx = blockchainService.generateContractCreateTx(
                    simpleContractBytecode,
                    encodedConstructor,
                    accountA.address
            )
            val signedTx = Numeric.toHexString(TransactionEncoder.signMessage(tx, accountA))
            simpleContractDeployTxHash = blockchainService.postTransaction(signedTx)
        }

        verify("Function can be called on deployed contract.") {
            val deployReceipt = blockchainService.getTransactionReceipt(simpleContractDeployTxHash)
            val contractAddress = deployReceipt.contractAddress
            val function = Function("get", emptyList(), listOf(TypeReference.create(Utf8String::class.java)))
            val encodedFunction = FunctionEncoder.encode(function)
            val callResult = blockchainService.contractCall(accountA.address, contractAddress, encodedFunction)
            val returnValues = FunctionReturnDecoder.decode(callResult, function.outputParameters)
            val textValue = returnValues.first() as Utf8String
            assert(storedMessage == textValue.value)
        }
    }
}
