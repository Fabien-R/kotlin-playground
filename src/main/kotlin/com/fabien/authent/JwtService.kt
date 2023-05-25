package com.fabien.authent

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import java.util.concurrent.TimeUnit

const val REGISTERED_AUTHORIZATION = "registered"
const val STAFF_AUTHORIZATION = "staff"

interface JwtService {
    fun AuthenticationConfig.configure()
}

fun configureJwt(jwtAudience: String, jwtDomain: String) = object : JwtService {
    val jwkProvider = JwkProviderBuilder(jwtDomain)
        .cached(10, 24, TimeUnit.HOURS)
        .build()

    fun validateCredentials(credential: JWTCredential, permission: String?): JWTPrincipal? {
        val containsAudience = credential.payload.audience.contains(jwtAudience)
        val containsScope = permission.isNullOrBlank() || credential.payload.claims["permissions"]?.asArray(String::class.java)?.contains(permission) == true
        if (containsAudience && containsScope) {
            return JWTPrincipal(credential.payload)
        }
        return null
    }

    override fun AuthenticationConfig.configure() {
        jwt(REGISTERED_AUTHORIZATION) {
            verifier(jwkProvider)
            validate { credential -> validateCredentials(credential, null) }
        }
        jwt(STAFF_AUTHORIZATION) {
            verifier(jwkProvider)
            validate { credential -> validateCredentials(credential, "BO:*:write") }
        }
    }
}
