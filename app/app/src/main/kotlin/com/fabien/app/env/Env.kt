package com.fabien.app.env

import io.ktor.server.config.*

data class Env(val jwt: Jwt, val insee: Insee, val mindee: Mindee, val postgres: Postgres)
data class Jwt(val domain: String, val audience: String)
data class Insee(
    val baseApi: String,
    val siretApi: String,
    val authenticationApi: String,
    val base64ConsumerKeySecret: String,
    val tokenValiditySeconds: String,
)

data class Mindee(val apiKey: String)

data class Postgres(
    val port: Int,
    val host: String,
    val database: String,
    val user: String,
    val password: String,
    val enabled: Boolean = false,
)

@Suppress("ComplexRedundantLet")
fun loadConfiguration(applicationConfig: ApplicationConfig): Env {
    val insee = applicationConfig.config("insee").let { inseeConfig ->
        Insee(
            inseeConfig.property("baseApi").getString(),
            inseeConfig.property("siretApi").getString(),
            inseeConfig.property("authenticationApi").getString(),
            inseeConfig.property("base64ConsumerKeySecret").getString(),
            inseeConfig.property("tokenValiditySeconds").getString(),
        )
    }
    val jwt = applicationConfig.config("jwt").let { jwtConfig ->
        Jwt(
            jwtConfig.property("domain").getString(),
            jwtConfig.property("audience").getString(),
        )
    }

    val mindee = applicationConfig.config("mindee").let { mindeeConfig ->
        Mindee(
            mindeeConfig.property("apiKey").getString(),
        )
    }

    val postgres = Postgres(
        port = 5432,
        host = "dummy",
        database = "dummy",
        user = "dummy",
        password = "guess",
        enabled = false,
    )

    return Env(
        jwt,
        insee,
        mindee,
        postgres,
    )
}
