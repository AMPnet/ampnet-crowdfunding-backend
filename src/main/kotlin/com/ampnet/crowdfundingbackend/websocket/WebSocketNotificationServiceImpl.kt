package com.ampnet.crowdfundingbackend.websocket

import com.ampnet.crowdfundingbackend.websocket.pojo.TxStatusResponse
import org.springframework.stereotype.Service
import org.springframework.messaging.simp.SimpMessagingTemplate

@Service
class WebSocketNotificationServiceImpl(
    private val messagingTemplate: SimpMessagingTemplate
) : WebSocketNotificationService {

    override fun notifyTxBroadcast(txId: Int, status: String) {
        val response = TxStatusResponse(txId, status)
        messagingTemplate.convertAndSend("/tx_status/$txId", response)

        // TODO: remove if the channel with txId is working
        messagingTemplate.convertAndSend("/tx_status", response)
        return
    }
}
