package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.response.IdentyumTokenResponse
import com.ampnet.crowdfundingbackend.service.IdentyumService
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class IdentyumController(private val identyumService: IdentyumService) {

    companion object : KLogging()

    @GetMapping("/identyum/token")
    fun getIdentyumToken(): ResponseEntity<IdentyumTokenResponse> {
        logger.debug { "Received request to get Identyum token" }
        val token = identyumService.getToken()
        return ResponseEntity.ok(IdentyumTokenResponse(token))
    }

    @PostMapping("/identyum/user")
    fun postUserData(): ResponseEntity<Unit> {
        // TODO: store user
        return ResponseEntity.ok().build()
    }
}
