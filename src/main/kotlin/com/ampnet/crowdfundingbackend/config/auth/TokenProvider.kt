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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import java.io.Serializable
import java.util.Date

@Component
class TokenProvider(val applicationProperties: ApplicationProperties,
                    val objectMapper: ObjectMapper) : Serializable {

    private val userKey = "User"
    private val hidden = "Hidden"

    fun generateToken(authentication: Authentication): String {
        val principal = authentication.principal as UserPrincipal
        return Jwts.builder()
                .setSubject(principal.email)
                .claim(userKey, principal)
                .signWith(SignatureAlgorithm.HS256, applicationProperties.jwt.signingKey)
                .setIssuedAt(Date())
                .setExpiration(Date(System.currentTimeMillis() +
                        minutesToMilliSeconds(applicationProperties.jwt.validityInMinutes.toInt())))
                .compact()
    }

    @Throws(TokenException::class)
    fun getAuthentication(token: String): UsernamePasswordAuthenticationToken {
        try {
            val jwtParser = Jwts.parser().setSigningKey(applicationProperties.jwt.signingKey)
            val claimsJws = jwtParser.parseClaimsJws(token)
            val claims = claimsJws.body
            validateExpiration(claims)

            val userPrincipal = getUserPrincipal(claims)
            validateSubject(claims, userPrincipal.email)

            return UsernamePasswordAuthenticationToken(
                    userPrincipal, hidden, userPrincipal.authorities.map { SimpleGrantedAuthority(it) })
        } catch (ex: JwtException) {
            throw TokenException("Could not validate JWT token", ex)
        }
    }

    private fun validateSubject(claims: Claims, email: String) {
        val subject = claims.subject
        if (subject != email) {
            throw TokenException("Invalid subject. Subject is $subject but should be $email")
        }
    }

    private fun validateExpiration(claims: Claims) {
        val expiration = claims.expiration
        if (Date().after(expiration)) {
            throw TokenException("Token expired. Expiration: $expiration")
        }
    }

    private fun getUserPrincipal(claims: Claims): UserPrincipal {
        val principalClaims = claims[userKey]
        try {
            val principalString = objectMapper.writeValueAsString(principalClaims)
            return objectMapper.readValue(principalString)
        } catch (ex: MissingKotlinParameterException) {
            throw TokenException("Could not extract user principal from JWT token for key: $userKey", ex)
        }
    }

    private fun minutesToMilliSeconds(minutes: Int): Int = minutes * 60 * 1000
}
