package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.exception.InvalidRequestException
import com.ampnet.crowdfundingbackend.exception.ResourceAlreadyExistsException
import com.ampnet.crowdfundingbackend.persistence.model.Withdraw
import com.ampnet.crowdfundingbackend.persistence.repository.WithdrawRepository
import com.ampnet.crowdfundingbackend.service.impl.TransactionInfoServiceImpl
import com.ampnet.crowdfundingbackend.service.impl.WithdrawServiceImpl
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.time.ZonedDateTime
import java.util.UUID

class WithdrawServiceTest : JpaServiceTestBase() {

    @Autowired
    lateinit var withdrawRepository: WithdrawRepository

    private val withdrawService: WithdrawService by lazy {
        val transactionInfoService = TransactionInfoServiceImpl(transactionInfoRepository)
        WithdrawServiceImpl(withdrawRepository, userWalletRepository, mockedBlockchainService, transactionInfoService)
    }
    private lateinit var withdraw: Withdraw

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllWithdraws()
        databaseCleanerService.deleteAllWallets()
        createWalletForUser(userUuid, "user-wallet-hash")
    }

    /* Create */
    @Test
    fun mustThrowExceptionIfUserHasUnapprovedWithdraw() {
        suppose("User has created withdraw") {
            createWithdraw(userUuid)
        }

        verify("Service will throw exception when user tries to create new withdraw") {
            assertThrows<ResourceAlreadyExistsException> {
                withdrawService.createWithdraw(userUuid, 100L)
            }
        }
    }

    @Test
    fun mustThrowExceptionIfUserHasApprovedWithdraw() {
        suppose("User has approved withdraw") {
            createApprovedWithdraw(userUuid)
        }

        verify("Service will throw exception when user tries to create new withdraw") {
            assertThrows<ResourceAlreadyExistsException> {
                withdrawService.createWithdraw(userUuid, 100L)
            }
        }
    }

    /* Approve */
    @Test
    fun mustThrowExceptionForGeneratingApprovalTxForAnotherUser() {
        suppose("User created withdraw") {
            withdraw = createWithdraw(userUuid)
        }

        verify("Service will throw exception when another user tires to generate approval transaction") {
            assertThrows<InvalidRequestException> {
                withdrawService.generateApprovalTransaction(withdraw.id, UUID.randomUUID())
            }
        }
    }

    @Test
    fun mustThrowExceptionForGeneratingApprovalTxForApprovedWithdraw() {
        suppose("User has approved withdraw") {
            withdraw = createApprovedWithdraw(userUuid)
        }

        verify("Service will throw exception when user tries to generate approval transaction for approved withdraw") {
            assertThrows<InvalidRequestException> {
                withdrawService.generateApprovalTransaction(withdraw.id, userUuid)
            }
        }
    }

    @Test
    fun mustThrowExceptionForConfirmingApprovedTx() {
        suppose("User has approved withdraw") {
            withdraw = createApprovedWithdraw(userUuid)
        }

        verify("Service will throw exception when user tries to confirm already approved tx") {
            assertThrows<InvalidRequestException> {
                withdrawService.confirmApproval("signed-transaction", withdraw.id)
            }
        }
    }

    /* Burn */
    @Test
    fun mustThrowExceptionForGeneratingBurnTxBeforeApproval() {
        suppose("User created withdraw") {
            withdraw = createWithdraw(userUuid)
        }

        verify("Service will throw exception when user tries to generate burn tx for unapproved withdraw") {
            assertThrows<InvalidRequestException> {
                withdrawService.generateBurnTransaction(withdraw.id, userUuid)
            }
        }
    }

    @Test
    fun mustThrowExceptionForGeneratingBurnTxForBurnedWithdraw() {
        suppose("Withdraw is burned") {
            withdraw = createBurnedWithdraw(userUuid)
        }

        verify("Service will throw exception when user tries to generate burn tx for burned withdraw") {
            assertThrows<InvalidRequestException> {
                withdrawService.generateBurnTransaction(withdraw.id, userUuid)
            }
        }
    }

    @Test
    fun mustThrowExceptionForBurningUnapprovedWithdraw() {
        suppose("Withdraw is not approved") {
            withdraw = createWithdraw(userUuid)
        }

        verify("Service will throw exception when user tries to burn unapproved withdraw") {
            assertThrows<InvalidRequestException> {
                withdrawService.burn("signed-transaction", withdraw.id)
            }
        }
    }

    @Test
    fun mustThrowExceptionForBurningAlreadyBurnedWithdraw() {
        suppose("Withdraw is burned") {
            withdraw = createBurnedWithdraw(userUuid)
        }

        verify("Service will throw exception when user tries to generate burn tx for burned withdraw") {
            assertThrows<InvalidRequestException> {
                withdrawService.burn("signed-transaction", withdraw.id)
            }
        }
    }

    private fun createBurnedWithdraw(user: UUID): Withdraw {
        val withdraw = Withdraw(0, user, 100L, ZonedDateTime.now(), 0,
                "approved-tx", ZonedDateTime.now(),
                "burned-tx", ZonedDateTime.now(), UUID.randomUUID())
        return withdrawRepository.save(withdraw)
    }

    private fun createApprovedWithdraw(user: UUID): Withdraw {
        val withdraw = Withdraw(0, user, 100L, ZonedDateTime.now(), 0,
                "approved-tx", ZonedDateTime.now(),
                null, null, null)
        return withdrawRepository.save(withdraw)
    }

    private fun createWithdraw(user: UUID): Withdraw {
        val withdraw = Withdraw(0, user, 100L, ZonedDateTime.now(), 0,
                null, null, null, null, null)
        return withdrawRepository.save(withdraw)
    }
}
