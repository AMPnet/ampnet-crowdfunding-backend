package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.blockchain.BlockchainService
import com.ampnet.crowdfundingbackend.controller.pojo.request.WalletCreateRequest
import com.ampnet.crowdfundingbackend.enums.Currency
import com.ampnet.crowdfundingbackend.enums.TransactionType
import com.ampnet.crowdfundingbackend.enums.WalletType
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.InternalException
import com.ampnet.crowdfundingbackend.exception.ResourceAlreadyExistsException
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.persistence.model.Wallet
import com.ampnet.crowdfundingbackend.persistence.repository.OrganizationRepository
import com.ampnet.crowdfundingbackend.persistence.repository.ProjectRepository
import com.ampnet.crowdfundingbackend.persistence.repository.UserRepository
import com.ampnet.crowdfundingbackend.persistence.repository.WalletRepository
import com.ampnet.crowdfundingbackend.service.TransactionService
import com.ampnet.crowdfundingbackend.service.WalletService
import com.ampnet.crowdfundingbackend.service.pojo.CreateTransactionRequest
import com.ampnet.crowdfundingbackend.service.pojo.GenerateProjectWalletRequest
import com.ampnet.crowdfundingbackend.service.pojo.PostTransactionType
import com.ampnet.crowdfundingbackend.service.pojo.TransactionData
import mu.KLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Service
class WalletServiceImpl(
    private val walletRepository: WalletRepository,
    private val userRepository: UserRepository,
    private val projectRepository: ProjectRepository,
    private val organizationRepository: OrganizationRepository,
    private val blockchainService: BlockchainService,
    private val transactionService: TransactionService
) : WalletService {

    companion object : KLogging()

    @Transactional(readOnly = true)
    @Throws(InternalException::class)
    override fun getWalletBalance(wallet: Wallet): Long {
        return blockchainService.getBalance(wallet.hash)
    }

    @Transactional
    @Throws(ResourceAlreadyExistsException::class, InternalException::class)
    override fun createUserWallet(user: User, request: WalletCreateRequest): Wallet {
        throwExceptionIfUserAlreadyHasWallet(user)

        val txHash = blockchainService.addWallet(request.address, request.publicKey)
        val wallet = createWallet(txHash, WalletType.USER)
        user.wallet = wallet
        userRepository.save(user)
        return wallet
    }

    @Transactional
    override fun generateTransactionToCreateProjectWallet(project: Project, userId: Int): TransactionData {
        throwExceptionIfProjectHasWallet(project)
        val walletHash = project.createdBy.wallet?.hash
                ?: throw ResourceNotFoundException(ErrorCode.WALLET_MISSING, "User wallet is missing")
        val organizationWalletHash = project.organization.wallet?.hash
            ?: throw ResourceNotFoundException(ErrorCode.WALLET_MISSING, "Organization wallet is missing")

        val request = GenerateProjectWalletRequest(project, organizationWalletHash, walletHash)
        val data = blockchainService.generateProjectWalletTransaction(request)
        val tx = transactionService.createProjectTransaction(project.name, userId)
        // TODO: return tx

        return data
    }

    @Transactional
    @Throws(ResourceAlreadyExistsException::class)
    override fun createProjectWallet(project: Project, signedTransaction: String): Wallet {
        throwExceptionIfProjectHasWallet(project)
        val txHash = blockchainService.postTransaction(signedTransaction, PostTransactionType.PRJ_CREATE)
        val wallet = createWallet(txHash, WalletType.PROJECT)
        project.wallet = wallet
        projectRepository.save(project)
        // TODO: delete transaction
//        transactionService.deleteTransaction(id)
        return wallet
    }

    @Transactional
    override fun generateTransactionToCreateOrganizationWallet(
        organization: Organization,
        userId: Int
    ): TransactionData {
        throwExceptionIfOrganizationAlreadyHasWallet(organization)
        val walletHash = organization.createdByUser.wallet?.hash
                ?: throw ResourceNotFoundException(ErrorCode.WALLET_MISSING, "User wallet is missing")

        val data = blockchainService.generateAddOrganizationTransaction(walletHash, organization.name)
        val txRequest = CreateTransactionRequest(TransactionType.CREATE_ORG, organization.name, "Description", userId)
        val tx = transactionService.createOrgTransaction(organization.name, userId)
        // TODO: return tx

        return data
    }

    @Transactional
    override fun createOrganizationWallet(organization: Organization, signedTransaction: String): Wallet {
        throwExceptionIfOrganizationAlreadyHasWallet(organization)
        val txHash = blockchainService.postTransaction(signedTransaction, PostTransactionType.ORG_CREATE)
        val wallet = createWallet(txHash, WalletType.ORG)
        organization.wallet = wallet
        organizationRepository.save(organization)
        // TODO: delete transaction
//        transactionService.deleteTransaction(id)
        return wallet
    }

    private fun createWallet(hash: String, type: WalletType): Wallet {
        if (walletRepository.findByHash(hash).isPresent) {
            logger.error { "SAME HASH! Trying to create wallet: $type with existing hash: $hash" }
            throw ResourceAlreadyExistsException(ErrorCode.WALLET_HASH_EXISTS, "Wallet with hash: $hash already exists")
        }

        val wallet = Wallet::class.java.getConstructor().newInstance()
        wallet.hash = hash
        wallet.type = type
        wallet.currency = Currency.EUR
        wallet.createdAt = ZonedDateTime.now()
        return walletRepository.save(wallet)
    }

    private fun throwExceptionIfProjectHasWallet(project: Project) {
        project.wallet?.let {
            logger.info("Trying to create wallet for user: ${project.id} but user already has a wallet.")
            throw ResourceAlreadyExistsException(ErrorCode.WALLET_EXISTS,
                    "Project: ${project.name} already has a wallet.")
        }
    }

    private fun throwExceptionIfUserAlreadyHasWallet(user: User) {
        user.wallet?.let {
            logger.info("User: ${user.id} already has a wallet.")
            throw ResourceAlreadyExistsException(ErrorCode.WALLET_EXISTS, "User: ${user.email} already has a wallet.")
        }
    }

    private fun throwExceptionIfOrganizationAlreadyHasWallet(organization: Organization) {
        organization.wallet?.let {
            logger.info("Organization: ${organization.id} already has a wallet.")
            throw ResourceAlreadyExistsException(ErrorCode.WALLET_EXISTS,
                    "Organization: ${organization.name} already has a wallet.")
        }
    }
}
