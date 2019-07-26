package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.blockchain.BlockchainService
import com.ampnet.crowdfundingbackend.controller.pojo.request.WalletCreateRequest
import com.ampnet.crowdfundingbackend.enums.Currency
import com.ampnet.crowdfundingbackend.enums.WalletType
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.InternalException
import com.ampnet.crowdfundingbackend.exception.ResourceAlreadyExistsException
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.UserWallet
import com.ampnet.crowdfundingbackend.persistence.model.Wallet
import com.ampnet.crowdfundingbackend.persistence.repository.OrganizationRepository
import com.ampnet.crowdfundingbackend.persistence.repository.ProjectRepository
import com.ampnet.crowdfundingbackend.persistence.repository.UserWalletRepository
import com.ampnet.crowdfundingbackend.persistence.repository.WalletRepository
import com.ampnet.crowdfundingbackend.service.TransactionInfoService
import com.ampnet.crowdfundingbackend.service.WalletService
import com.ampnet.crowdfundingbackend.blockchain.pojo.GenerateProjectWalletRequest
import com.ampnet.crowdfundingbackend.service.pojo.PostTransactionType
import com.ampnet.crowdfundingbackend.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.crowdfundingbackend.persistence.model.PairWalletCode
import com.ampnet.crowdfundingbackend.persistence.repository.PairWalletCodeRepository
import mu.KLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import java.time.ZonedDateTime

@Service
class WalletServiceImpl(
    private val walletRepository: WalletRepository,
    private val projectRepository: ProjectRepository,
    private val organizationRepository: OrganizationRepository,
    private val userWalletRepository: UserWalletRepository,
    private val blockchainService: BlockchainService,
    private val transactionInfoService: TransactionInfoService,
    private val pairWalletCodeRepository: PairWalletCodeRepository
) : WalletService {

    companion object : KLogging()

    private val charPool: List<Char> = ('A'..'Z') + ('0'..'9')

    @Transactional(readOnly = true)
    @Throws(InternalException::class)
    override fun getWalletBalance(wallet: Wallet): Long {
        return blockchainService.getBalance(wallet.hash)
    }

    @Transactional(readOnly = true)
    override fun getUserWallet(userUuid: UUID): Wallet? {
        return ServiceUtils.wrapOptional(userWalletRepository.findByUserUuid(userUuid))?.wallet
    }

    @Transactional
    @Throws(ResourceAlreadyExistsException::class, InternalException::class)
    override fun createUserWallet(userUuid: UUID, request: WalletCreateRequest): Wallet {
        userWalletRepository.findByUserUuid(userUuid).ifPresent {
            throw ResourceAlreadyExistsException(ErrorCode.WALLET_EXISTS, "User: $userUuid already has a wallet.")
        }
        pairWalletCodeRepository.findByAddress(request.address).ifPresent {
            pairWalletCodeRepository.delete(it)
        }

        val txHash = blockchainService.addWallet(request.address, request.publicKey)
        val wallet = createWallet(txHash, WalletType.USER)
        val userWallet = UserWallet(0, userUuid, wallet)
        userWalletRepository.save(userWallet)
        return wallet
    }

    @Transactional
    override fun generateTransactionToCreateProjectWallet(project: Project, userUuid: UUID): TransactionDataAndInfo {
        throwExceptionIfProjectHasWallet(project)
        val userWalletHash = getUserWallet(userUuid)?.hash
                ?: throw ResourceNotFoundException(ErrorCode.WALLET_MISSING, "User wallet is missing")
        val organizationWalletHash = project.organization.wallet?.hash
            ?: throw ResourceNotFoundException(ErrorCode.WALLET_MISSING, "Organization wallet is missing")

        val request = GenerateProjectWalletRequest(project, organizationWalletHash, userWalletHash)
        val data = blockchainService.generateProjectWalletTransaction(request)
        val info = transactionInfoService.createProjectTransaction(project, userUuid)
        return TransactionDataAndInfo(data, info)
    }

    @Transactional
    @Throws(ResourceAlreadyExistsException::class)
    override fun createProjectWallet(project: Project, signedTransaction: String): Wallet {
        throwExceptionIfProjectHasWallet(project)
        val txHash = blockchainService.postTransaction(signedTransaction, PostTransactionType.PRJ_CREATE)
        val wallet = createWallet(txHash, WalletType.PROJECT)
        project.wallet = wallet
        projectRepository.save(project)
        return wallet
    }

    @Transactional
    override fun generateTransactionToCreateOrganizationWallet(
        organization: Organization,
        userUuid: UUID
    ): TransactionDataAndInfo {
        throwExceptionIfOrganizationAlreadyHasWallet(organization)
        val walletHash = getUserWallet(userUuid)?.hash
                ?: throw ResourceNotFoundException(ErrorCode.WALLET_MISSING, "User wallet is missing")

        val data = blockchainService.generateAddOrganizationTransaction(walletHash, organization.name)
        val info = transactionInfoService.createOrgTransaction(organization, userUuid)
        return TransactionDataAndInfo(data, info)
    }

    @Transactional
    override fun createOrganizationWallet(organization: Organization, signedTransaction: String): Wallet {
        throwExceptionIfOrganizationAlreadyHasWallet(organization)
        val txHash = blockchainService.postTransaction(signedTransaction, PostTransactionType.ORG_CREATE)
        val wallet = createWallet(txHash, WalletType.ORG)
        organization.wallet = wallet
        organizationRepository.save(organization)
        return wallet
    }

    @Transactional
    override fun generatePairWalletCode(request: WalletCreateRequest): PairWalletCode {
        pairWalletCodeRepository.findByAddress(request.address).ifPresent {
            pairWalletCodeRepository.delete(it)
        }
        val code = generatePairWalletCode()
        val pairWalletCode = PairWalletCode(0, request.address, request.publicKey, code, ZonedDateTime.now())
        return pairWalletCodeRepository.save(pairWalletCode)
    }

    @Transactional(readOnly = true)
    override fun getPairWalletCode(code: String): PairWalletCode? {
        return ServiceUtils.wrapOptional(pairWalletCodeRepository.findByCode(code))
    }

    private fun createWallet(hash: String, type: WalletType): Wallet {
        if (walletRepository.findByHash(hash).isPresent) {
            throw ResourceAlreadyExistsException(ErrorCode.WALLET_HASH_EXISTS,
                    "SAME HASH! Trying to create wallet: $type with existing hash: $hash")
        }
        val wallet = Wallet(0, hash, type, Currency.EUR, ZonedDateTime.now())
        return walletRepository.save(wallet)
    }

    private fun throwExceptionIfProjectHasWallet(project: Project) {
        project.wallet?.let {
            throw ResourceAlreadyExistsException(ErrorCode.WALLET_EXISTS,
                    "Project: ${project.name} already has a wallet.")
        }
    }

    private fun throwExceptionIfOrganizationAlreadyHasWallet(organization: Organization) {
        organization.wallet?.let {
            throw ResourceAlreadyExistsException(ErrorCode.WALLET_EXISTS,
                    "Organization: ${organization.name} already has a wallet.")
        }
    }

    private fun generatePairWalletCode(): String = (1..6)
        .map { kotlin.random.Random.nextInt(0, charPool.size) }
        .map(charPool::get)
        .joinToString("")
}
