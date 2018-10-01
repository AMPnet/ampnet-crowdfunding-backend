package com.ampnet.crowdfundingbackend.config

import com.ampnet.crowdfundingbackend.persistence.model.User
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import java.io.Serializable
import java.util.Date
import java.util.stream.Collectors

@Component
class TokenProvider(val applicationProperties: ApplicationProperties) : Serializable {

    fun getUsernameFromToken(token: String): String {
        return getClaimFromToken(token, Claims::getSubject)
    }

    fun getExpirationDateFromToken(token: String): Date {
        return getClaimFromToken(token, Claims::getExpiration)
    }

    fun <T : Any> getClaimFromToken(token: String, claimsResolver: (Claims) -> T): T {
        val claims = getAllClaimsFromToken(token)
        return claimsResolver.invoke(claims)
    }

    fun getAllClaimsFromToken(token: String): Claims {
        return Jwts.parser()
                .setSigningKey(applicationProperties.jwt.signingKey)
                .parseClaimsJws(token)
                .body
    }

    fun isTokenExpired(token: String): Boolean {
        val expiration = getExpirationDateFromToken(token)
        return expiration.before(Date())
    }

    fun generateToken(authentication: Authentication): String {
        val authorities = authentication.authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","))
        return Jwts.builder()
                .setSubject(authentication.name)
                .claim(applicationProperties.jwt.authoritiesKey, authorities)
                .signWith(SignatureAlgorithm.HS256, applicationProperties.jwt.signingKey)
                .setIssuedAt(Date())
                .setExpiration(Date(System.currentTimeMillis() +
                        minutesToMilliSeconds(applicationProperties.jwt.validityInMinutes.toInt())))
                .compact()
    }

    fun validateToken(token: String, userDetails: User): Boolean {
        val username = getUsernameFromToken(token)
        return username == userDetails.email && !isTokenExpired(token)
    }

    fun getAuthentication(token: String, userDetails: User): UsernamePasswordAuthenticationToken {
        val jwtParser = Jwts.parser().setSigningKey(applicationProperties.jwt.signingKey)
        val claimsJws = jwtParser.parseClaimsJws(token)
        val claims = claimsJws.body
        val authorities =
                claims[applicationProperties.jwt.authoritiesKey].toString()
                        .split(",").toTypedArray()
                        .map { SimpleGrantedAuthority(it) }
                        .toList()
        return UsernamePasswordAuthenticationToken(userDetails, "", authorities)
    }

    private fun minutesToMilliSeconds(minutes: Int): Int = minutes * 60 * 1000
}
