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
    // TODO Fabien plug postgres DB
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
