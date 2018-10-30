package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.exception.ResourceAlreadyExistsException
import com.ampnet.crowdfundingbackend.persistence.model.Currency
import com.ampnet.crowdfundingbackend.persistence.model.Wallet
import com.ampnet.crowdfundingbackend.persistence.repository.TransactionDao
import com.ampnet.crowdfundingbackend.persistence.repository.WalletDao
import com.ampnet.crowdfundingbackend.service.WalletService
import mu.KLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Service
class WalletServiceImpl(val walletDao: WalletDao, val transactionDao: TransactionDao) : WalletService {

    companion object : KLogging()

    @Transactional
    override fun createWallet(ownerId: Int): Wallet {
        if (walletDao.findByOwnerId(ownerId).isPresent) {
            logger.info("Trying to save wallet with ownerId: $ownerId which already exists in db.")
            throw ResourceAlreadyExistsException("Wallet with ownerId: $ownerId already exits")
        }

        val wallet = Wallet::class.java.newInstance()
        wallet.ownerId = ownerId
        wallet.currency = Currency.EUR
        wallet.createdAt = ZonedDateTime.now()
        wallet.transactions = emptyList()
        return walletDao.save(wallet)
    }

    @Transactional(readOnly = true)
    override fun getWalletForUser(userId: Int): Wallet? {
        return ServiceUtils.wrapOptional(walletDao.findByOwnerId(userId))
    }
}
