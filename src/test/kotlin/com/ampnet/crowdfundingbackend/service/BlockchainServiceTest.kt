package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.service.impl.BlockchainServiceImpl
import net.devh.springboot.autoconfigure.grpc.client.GrpcChannelFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.math.BigDecimal

class BlockchainServiceTest {

    private val service = BlockchainServiceImpl(Mockito.mock(GrpcChannelFactory::class.java))

    @Test
    fun mustTransformEuroToCents() {
        val euro = BigDecimal("1001.99")
        val cents = service.transformEuroToCents(euro)
        assertThat(cents).isEqualTo(100199)
    }

    @Test
    fun mustTransformCentsToEuro() {
        val cents: Long = 1001
        val euro = service.transformCentsToEuro(cents)
        assertThat(euro).isEqualTo(BigDecimal("10.01"))
    }

    @Test
    fun maxCentsToEuro() {
        val cents = Long.MAX_VALUE // 9223372036854775807
        val euro = service.transformCentsToEuro(cents)
        assertThat(euro).isEqualTo(BigDecimal("92233720368547758.07"))
    }
}
