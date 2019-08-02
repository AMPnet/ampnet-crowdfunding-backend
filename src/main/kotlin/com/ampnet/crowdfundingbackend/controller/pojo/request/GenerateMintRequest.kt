package com.ampnet.crowdfundingbackend.controller.pojo.request

data class GenerateMintRequest(val toWallet: String, val amount: Long, val depositId: Int)
