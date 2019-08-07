package com.ampnet.crowdfundingbackend.service.pojo

import java.util.UUID

data class MintServiceRequest(val amount: Long, val byUser: UUID, val depositId: Int)
