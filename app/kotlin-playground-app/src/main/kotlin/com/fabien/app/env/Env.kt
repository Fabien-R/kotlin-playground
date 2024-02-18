package com.fabien.app.env

import com.fabien.domain.Insee
import com.fabien.domain.Jwt
import com.fabien.domain.Mindee
import com.fabien.domain.Postgres
import io.ktor.server.config.*

data class Env(val jwt: Jwt, val insee: Insee, val mindee: Mindee, val postgres: Postgres)

@Suppress("ComplexRedundantLet")
fun loadConfiguration(applicationConfig: ApplicationConfig): Env {
    val insee = applicationConfig.config("insee").let { inseeConfig ->
        val base64keySecret =
            "${inseeConfig.property("consumerKey").getString()}:${inseeConfig.property("consumerSecret").getString()}"
                .toByteArray()
                .let { java.util.Base64.getEncoder().encodeToString(it) }
        Insee(
            inseeConfig.property("baseApi").getString(),
            inseeConfig.property("siretApi").getString(),
            inseeConfig.property("authenticationApi").getString(),
            base64keySecret,
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

    val postgres = applicationConfig.config("postgres").let { postgresConfig ->
        Postgres(
            port = postgresConfig.property("port").getString().toInt(),
            host = postgresConfig.property("host").getString(),
            database = postgresConfig.property("database").getString(),
            user = postgresConfig.property("user").getString(),
            password = postgresConfig.property("password").getString(),
            enabled = postgresConfig.property("enabled").getString().toBoolean(),
        )
    }

    return Env(
        jwt,
        insee,
        mindee,
        postgres,
    )
}
