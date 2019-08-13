package com.ampnet.crowdfundingbackend.websocket

interface WebSocketNotificationService {
    fun notifyTxBroadcast(txId: Int, status: String)
}
