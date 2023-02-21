package com.example.plugins

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.concurrent.TimeUnit

fun Application.configureSecurity() {
    fun validateCredentials(credential: JWTCredential, permission: String?): JWTPrincipal? {
        val containsAudience = credential.payload.audience.contains(this@configureSecurity.environment.config.property("jwt.audience").getString())
        val containsScope = permission.isNullOrBlank() || credential.payload.claims["permissions"]?.asArray(String::class.java)?.contains(permission) == true
        if (containsAudience && containsScope) {
            return JWTPrincipal(credential.payload)
        }
        return null
    }

    val jwkProvider = JwkProviderBuilder(this@configureSecurity.environment.config.property("jwt.domain").getString())
        .cached(10, 24, TimeUnit.HOURS)
        .build()

    authentication {
        jwt("auth0") {
            verifier(jwkProvider)
            validate { credential -> validateCredentials(credential, null) }
        }
    }
    routing {
        authenticate("auth0") {
            get("/secured") {
                call.respondText { "Hey you" }
            }
        }

    }
}
