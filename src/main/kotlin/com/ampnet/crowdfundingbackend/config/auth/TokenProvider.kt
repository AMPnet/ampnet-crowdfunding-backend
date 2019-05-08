package com.ampnet.crowdfundingbackend.config.auth

import com.ampnet.crowdfundingbackend.config.ApplicationProperties
import com.ampnet.crowdfundingbackend.exception.TokenException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.fasterxml.jackson.module.kotlin.readValue
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.io.JacksonDeserializer
import io.jsonwebtoken.io.JacksonSerializer
import io.jsonwebtoken.security.Keys
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import java.io.Serializable
import java.util.Date
import javax.crypto.SecretKey

@Component
class TokenProvider(val applicationProperties: ApplicationProperties, val objectMapper: ObjectMapper) : Serializable {

    private val userKey = "user"
    private val hidden = "Hidden"

    private val key: SecretKey = Keys.hmacShaKeyFor(applicationProperties.jwt.signingKey.toByteArray())

    fun generateToken(authentication: Authentication): String {
        val principal = authentication.principal as? UserPrincipal
                ?: throw TokenException("Authentication principal must be UserPrincipal")
        return Jwts.builder()
                .serializeToJsonWith(JacksonSerializer(objectMapper))
                .setSubject(principal.email)
                .claim(userKey, objectMapper.writeValueAsString(principal))
                .signWith(key, SignatureAlgorithm.HS256)
                .setIssuedAt(Date())
                .setExpiration(Date(System.currentTimeMillis() +
                        minutesToMilliSeconds(applicationProperties.jwt.validityInMinutes)))
                .compact()
    }

    @Throws(TokenException::class)
    fun getAuthentication(token: String): UsernamePasswordAuthenticationToken {
        try {
            val jwtParser = Jwts.parser()
                    .deserializeJsonWith(JacksonDeserializer(objectMapper))
                    .setSigningKey(key)
            val claimsJws = jwtParser.parseClaimsJws(token)
            val claims = claimsJws.body
            validateExpiration(claims)

            val userPrincipal = getUserPrincipal(claims)
            return UsernamePasswordAuthenticationToken(
                    userPrincipal, hidden, userPrincipal.authorities.map { SimpleGrantedAuthority(it) })
        } catch (ex: JwtException) {
            throw TokenException("Could not validate JWT token", ex)
        }
    }

    private fun validateExpiration(claims: Claims) {
        val expiration = claims.expiration
        if (Date().after(expiration)) {
            throw TokenException("Token expired. Expiration: $expiration")
        }
    }

    private fun getUserPrincipal(claims: Claims): UserPrincipal {
        val principalClaims = claims[userKey] as? String
                ?: throw TokenException("Token principal claims in invalid format")
        try {
            return objectMapper.readValue(principalClaims)
        } catch (ex: MissingKotlinParameterException) {
            throw TokenException("Could not extract user principal from JWT token for key: $userKey", ex)
        }
    }

    private fun minutesToMilliSeconds(minutes: Int): Int = minutes * 60 * 1000
}
