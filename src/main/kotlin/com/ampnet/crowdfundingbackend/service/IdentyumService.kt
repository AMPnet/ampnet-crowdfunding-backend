package com.ampnet.crowdfundingbackend.service

interface IdentyumService {
    fun getToken(): String
    fun storeUser()
}
