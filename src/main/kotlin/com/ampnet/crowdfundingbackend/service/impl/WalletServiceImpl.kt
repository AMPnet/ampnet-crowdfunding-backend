package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.exception.ResourceAlreadyExistsException
import com.ampnet.crowdfundingbackend.enums.Currency
import com.ampnet.crowdfundingbackend.enums.WalletType
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.persistence.model.Wallet
import com.ampnet.crowdfundingbackend.persistence.repository.ProjectRepository
import com.ampnet.crowdfundingbackend.persistence.repository.UserRepository
import com.ampnet.crowdfundingbackend.persistence.repository.WalletRepository
import com.ampnet.crowdfundingbackend.blockchain.BlockchainService
import com.ampnet.crowdfundingbackend.controller.pojo.request.WalletCreateRequest
import com.ampnet.crowdfundingbackend.exception.InternalException
import com.ampnet.crowdfundingbackend.exception.InvalidRequestException
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.WalletToken
import com.ampnet.crowdfundingbackend.persistence.repository.OrganizationRepository
import com.ampnet.crowdfundingbackend.persistence.repository.WalletTokenRepository
import com.ampnet.crowdfundingbackend.service.WalletService
import com.ampnet.crowdfundingbackend.service.pojo.GenerateProjectWalletRequest
import com.ampnet.crowdfundingbackend.service.pojo.TransactionData
import mu.KLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

@Service
class WalletServiceImpl(
    private val walletRepository: WalletRepository,
    private val userRepository: UserRepository,
    private val projectRepository: ProjectRepository,
    private val organizationRepository: OrganizationRepository,
    private val walletTokenRepository: WalletTokenRepository,
    private val blockchainService: BlockchainService
) : WalletService {

    companion object : KLogging()

    @Transactional(readOnly = true)
    @Throws(InternalException::class)
    override fun getWalletBalance(wallet: Wallet): Long {
        return blockchainService.getBalance(wallet.hash)
                ?: throw InternalException(ErrorCode.INT_WALLET_FUNDS, "Could not fetch wallet funds from blockchain")
    }

    @Transactional
    @Throws(ResourceAlreadyExistsException::class, InternalException::class)
    override fun createUserWallet(request: WalletCreateRequest): Wallet {
        val token = ServiceUtils.wrapOptional(walletTokenRepository.findByToken(UUID.fromString(request.token)))
                ?: throw ResourceNotFoundException(ErrorCode.WALLET_TOKEN_MISSING, "Missing token: ${request.token}")
        if (token.isExpired()) {
            throw InvalidRequestException(ErrorCode.WALLET_TOKEN_EXPIRED, "Wallet token has expired!")
        }

        val user = token.user
        throwExceptionIfUserAlreadyHasWallet(user)

        val txHash = blockchainService.addWallet(request.address)
                ?: throw InternalException(ErrorCode.INT_WALLET_ADD, "Could not store User wallet")
        val wallet = createWallet(txHash, WalletType.USER)
        user.wallet = wallet
        userRepository.save(user)
        walletTokenRepository.delete(token)
        return wallet
    }

    @Transactional
    override fun createWalletToken(user: User): WalletToken {
        throwExceptionIfUserAlreadyHasWallet(user)
        walletTokenRepository.findByUserId(user.id).ifPresent {
            walletTokenRepository.delete(it)
        }

        val token = WalletToken::class.java.newInstance()
        token.user = user
        token.token = UUID.randomUUID()
        token.createdAt = ZonedDateTime.now()
        return walletTokenRepository.save(token)
    }

    @Transactional(readOnly = true)
    override fun generateTransactionToCreateProjectWallet(project: Project): TransactionData {
        throwExceptionIfProjectHasWallet(project)
        val walletHash = project.createdBy.wallet?.hash
                ?: throw ResourceNotFoundException(ErrorCode.WALLET_MISSING, "User wallet is missing")
        val organizationWalletHash = project.organization.wallet?.hash
            ?: throw ResourceNotFoundException(ErrorCode.WALLET_MISSING, "Organization wallet is missing")

        val request = GenerateProjectWalletRequest(project, organizationWalletHash, walletHash)
        return blockchainService.generateProjectWalletTransaction(request)
    }

    @Transactional
    @Throws(ResourceAlreadyExistsException::class)
    override fun createProjectWallet(project: Project, signedTransaction: String): Wallet {
        throwExceptionIfProjectHasWallet(project)
        val txHash = blockchainService.postTransaction(signedTransaction)
        val wallet = createWallet(txHash, WalletType.PROJECT)
        project.wallet = wallet
        projectRepository.save(project)
        return wallet
    }

    @Transactional(readOnly = true)
    override fun generateTransactionToCreateOrganizationWallet(organization: Organization): TransactionData {
        throwExceptionIfOrganizationAlreadyHasWallet(organization)
        val walletHash = organization.createdByUser.wallet?.hash
                ?: throw ResourceNotFoundException(ErrorCode.WALLET_MISSING, "User wallet is missing")

        return blockchainService.generateAddOrganizationTransaction(walletHash, organization.name)
    }

    @Transactional
    override fun createOrganizationWallet(organization: Organization, signedTransaction: String): Wallet {
        throwExceptionIfOrganizationAlreadyHasWallet(organization)
        val txHash = blockchainService.postTransaction(signedTransaction)
        val wallet = createWallet(txHash, WalletType.ORG)
        organization.wallet = wallet
        organizationRepository.save(organization)
        return wallet
    }

    private fun createWallet(hash: String, type: WalletType): Wallet {
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
