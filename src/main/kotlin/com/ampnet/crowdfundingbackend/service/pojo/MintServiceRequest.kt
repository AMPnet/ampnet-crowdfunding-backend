package com.ampnet.crowdfundingbackend.service.pojo

import java.util.UUID

data class MintServiceRequest(val toWallet: String, val amount: Long, val byUser: UUID, val depositId: Int)
