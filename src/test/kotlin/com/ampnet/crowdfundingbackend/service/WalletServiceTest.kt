package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.exception.ResourceAlreadyExistsException
import com.ampnet.crowdfundingbackend.enums.Currency
import com.ampnet.crowdfundingbackend.enums.WalletType
import com.ampnet.crowdfundingbackend.persistence.model.User
import com.ampnet.crowdfundingbackend.service.impl.UserServiceImpl
import com.ampnet.crowdfundingbackend.service.impl.WalletServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito

class WalletServiceTest : JpaServiceTestBase() {

    private val userService: UserService by lazy {
        val mailService = Mockito.mock(MailService::class.java)
        UserServiceImpl(userRepository, roleRepository, countryRepository, mailTokenRepository,
                mailService, passwordEncoder, applicationProperties)
    }
    private val walletService: WalletService by lazy {
        WalletServiceImpl(walletRepository, userRepository)
    }
    private lateinit var user: User

    private val defaultAddress = "0x14bC6a8219c798394726f8e86E040A878da1d99D"

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllWalletsAndOwners()
        user = createUser("test@email.com", "First", "Last")
    }

    @Test
    fun mustBeAbleToGetWalletForUserId() {
        suppose("User has a wallet") {
            createWalletForUser(user, defaultAddress)
        }

        verify("Service must fetch wallet for user with id") {
            val user = userService.findWithWallet(user.email)
            assertThat(user).isNotNull
            assertThat(user!!.wallet).isNotNull
            assertThat(user.wallet!!.address).isEqualTo(defaultAddress)
            assertThat(user.wallet!!.currency).isEqualTo(Currency.EUR)
            assertThat(user.wallet!!.type).isEqualTo(WalletType.USER)
        }
    }

    @Test
    fun mustBeAbleToCreateWalletForUser() {
        verify("Service can create wallet for a user") {
            val wallet = createWalletForUser(user, defaultAddress)
            assertThat(wallet).isNotNull
            assertThat(wallet.address).isEqualTo(defaultAddress)
            assertThat(wallet.currency).isEqualTo(Currency.EUR)
            assertThat(wallet.type).isEqualTo(WalletType.USER)
        }
        verify("Wallet is assigned to the user") {
            val userWithWallet = userService.findWithWallet(user.email)
            assertThat(userWithWallet).isNotNull
            assertThat(userWithWallet!!.wallet).isNotNull
            assertThat(userWithWallet.wallet!!.address).isEqualTo(defaultAddress)
        }
    }

    @Test
    fun mustNotBeAbleToCreateMultipleWalletsForOneUser() {
        suppose("User has a wallet") {
            createWalletForUser(user, defaultAddress)
        }

        verify("Service cannot create additional account") {
            assertThrows<ResourceAlreadyExistsException> { walletService.createUserWallet(user, defaultAddress) }
        }
    }
}
