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
class TokenProvider : Serializable {

    val SIGNING_KEY = "dujma"
    val AUTHORITIES_KEY = "scopes"
    val ACCESS_TOKEN_VALIDITY_SECONDS = 5 * 60 * 60

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
                .setSigningKey(SIGNING_KEY)
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
                .claim(AUTHORITIES_KEY, authorities)
                .signWith(SignatureAlgorithm.HS256, SIGNING_KEY)
                .setIssuedAt(Date())
                .setExpiration(Date(System.currentTimeMillis() + ACCESS_TOKEN_VALIDITY_SECONDS * 1000))
                .compact()
    }

    fun validateToken(token: String, userDetails: User): Boolean {
        val username = getUsernameFromToken(token)
        return username.equals(userDetails.email) && !isTokenExpired(token)
    }

    fun getAuthentication(token: String, userDetails: User): UsernamePasswordAuthenticationToken {
        val jwtParser = Jwts.parser().setSigningKey(SIGNING_KEY)
        val claimsJws = jwtParser.parseClaimsJws(token)
        val claims = claimsJws.body
        val authorities =
                claims.get(AUTHORITIES_KEY).toString()
                        .split(",").toTypedArray()
                        .map { SimpleGrantedAuthority(it) }
                        .toList()
        return UsernamePasswordAuthenticationToken(userDetails, "", authorities)
    }
}