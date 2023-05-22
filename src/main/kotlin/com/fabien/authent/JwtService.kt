package com.fabien.authent

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import java.util.concurrent.TimeUnit

const val AUTH0 = "auth0"
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
        jwt(AUTH0) {
            verifier(jwkProvider)
            validate { credential -> validateCredentials(credential, null) }
        }
    }
}
