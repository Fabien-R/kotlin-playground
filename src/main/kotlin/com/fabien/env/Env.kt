package com.fabien.env

import io.ktor.server.config.*

data class Env(val jwt: Jwt, val insee: Insee)
data class Jwt(val domain: String, val audience: String)
data class Insee(val baseApi: String, val siretApi: String, val authenticationApi: String, val base64ConsumerKeySecret: String, val tokenValiditySeconds: String)

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

    return Env(
        jwt,
        insee,
    )
}
