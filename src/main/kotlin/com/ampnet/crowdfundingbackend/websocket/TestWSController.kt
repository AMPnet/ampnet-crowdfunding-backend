package com.ampnet.crowdfundingbackend.websocket

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

// TODO: remove after testing
@RestController
class TestWSController(private val notificationService: WebSocketNotificationService) {

    @GetMapping("/websocket/test")
    fun test(): ResponseEntity<Unit> {
        notificationService.notifyTxBroadcast(1, "SUCCESS")
        return ResponseEntity.ok().build()
    }
}
