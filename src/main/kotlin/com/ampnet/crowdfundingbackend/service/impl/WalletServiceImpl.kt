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
import com.ampnet.crowdfundingbackend.service.BlockchainService
import com.ampnet.crowdfundingbackend.service.WalletService
import mu.KLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Service
class WalletServiceImpl(
    private val walletRepository: WalletRepository,
    private val userRepository: UserRepository,
    private val projectRepository: ProjectRepository,
    private val blockchainService: BlockchainService
) : WalletService {

    companion object : KLogging()

    @Transactional(readOnly = true)
    override fun getWalletBalance(wallet: Wallet): Long {
        return blockchainService.getBalance(wallet.address)
    }

    @Transactional
    @Throws(ResourceAlreadyExistsException::class)
    override fun createUserWallet(user: User, address: String): Wallet {
        user.wallet?.let {
            logger.info("Trying to create wallet for user: ${user.id} but user already has a wallet.")
            throw ResourceAlreadyExistsException(ErrorCode.WALLET_EXISTS,
                    "User: ${user.email} already has a wallet.")
        }
        val wallet = createWallet(address, WalletType.USER)
        user.wallet = wallet
        userRepository.save(user)
        return wallet
    }

    @Transactional
    @Throws(ResourceAlreadyExistsException::class)
    override fun createProjectWallet(project: Project, address: String): Wallet {
        project.wallet?.let {
            logger.info("Trying to create wallet for user: ${project.id} but user already has a wallet.")
            throw ResourceAlreadyExistsException(ErrorCode.WALLET_EXISTS,
                    "Project: ${project.name} already has a wallet.")
        }
        val wallet = createWallet(address, WalletType.PROJECT)
        project.wallet = wallet
        projectRepository.save(project)
        return wallet
    }

    private fun createWallet(address: String, type: WalletType): Wallet {
        val wallet = Wallet::class.java.getConstructor().newInstance()
        wallet.address = address
        wallet.type = type
        wallet.currency = Currency.EUR
        wallet.createdAt = ZonedDateTime.now()
        return walletRepository.save(wallet)
    }
}
