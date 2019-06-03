package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.config.ApplicationProperties
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.InternalException
import com.ampnet.crowdfundingbackend.service.IdentyumService
import mu.KLogging
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity

@Service
class IdentyumServiceImpl(
    private val applicationProperties: ApplicationProperties,
    private val restTemplate: RestTemplate
) : IdentyumService {

    companion object : KLogging()

    private val fieldUsername = "username"
    private val fieldPassword = "password"

    override fun getToken(): String {
        val request = generateIdentyumRequest()
        try {
            val response = restTemplate.postForEntity<String>(applicationProperties.identyum.url, request)
            if (response.statusCode.is2xxSuccessful) {
                response.body?.let {
                    return it
                }
            }
            throw InternalException(ErrorCode.REG_IDENTYUM,
                    "Could not get Identyum token. Status code: ${response.statusCode.value()}. Body: ${response.body}")
        } catch (ex: RestClientException) {
            throw InternalException(ErrorCode.REG_IDENTYUM, "Could not reach Identyum", ex)
        }
    }

    override fun storeUser() {
        // TODO: decrypt user data from identyum, store user and remove identyum token
    }

    private fun generateIdentyumRequest(): HttpEntity<MultiValueMap<String, String>> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED
        val map = LinkedMultiValueMap<String, String>()
        map[fieldUsername] = applicationProperties.identyum.username
        map[fieldPassword] = applicationProperties.identyum.password
        return HttpEntity(map, headers)
    }
}
