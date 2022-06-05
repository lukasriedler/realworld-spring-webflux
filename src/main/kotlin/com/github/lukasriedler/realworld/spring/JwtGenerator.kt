package com.github.lukasriedler.realworld.spring

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTCreationException
import com.auth0.jwt.exceptions.JWTDecodeException
import com.auth0.jwt.exceptions.JWTVerificationException
import org.springframework.stereotype.Component

@Component
class JwtGenerator {

    private val secret = "secret" // TODO actual secret handling
    private val issuer = "com.github.lukasriedler.realworld"
    private val algorithm = Algorithm.HMAC256(secret)

    fun generateToken(email: String): String? {
        return try {
            JWT.create()
                .withIssuer(issuer)
                .withClaim("email", email)
                .sign(algorithm)
        } catch (e: JWTCreationException) {
            null
        }
    }

    fun verifyToken(token: String): Boolean {
        val jwtVerifier = JWT.require(algorithm)
            .withIssuer(issuer)
            .withClaimPresence("email")
            .build()
        return try {
            jwtVerifier.verify(token)
            true
        } catch (e: JWTVerificationException) {
            false
        }
    }

    fun emailFromToken(token: String): String? {
        try {
            val decodedJWT = JWT.decode(token)
            val emailClaim = decodedJWT.claims["email"] ?: return null
            return emailClaim.asString()
        } catch (e: JWTDecodeException) {
            return null
        }
    }
}