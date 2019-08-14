package com.ampnet.crowdfundingbackend.websocket

import com.ampnet.crowdfundingbackend.websocket.pojo.TxStatusResponse
import mu.KLogging
import org.springframework.messaging.MessagingException
import org.springframework.stereotype.Service
import org.springframework.messaging.simp.SimpMessagingTemplate

@Service
class WebSocketNotificationServiceImpl(
    private val messagingTemplate: SimpMessagingTemplate
) : WebSocketNotificationService {

    companion object : KLogging()

    override fun notifyTxBroadcast(txId: Int, status: String) {
        val response = TxStatusResponse(txId, status)
        logger.debug { "Sending WebSocket notification: $response" }
        try {
            messagingTemplate.convertAndSend("/tx_status/$txId", response)
        } catch (ex: MessagingException) {
            logger.warn(ex) { "Failed to send WebSocket notification: $response" }
        }
    }
}
