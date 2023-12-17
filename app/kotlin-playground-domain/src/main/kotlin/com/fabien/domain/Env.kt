package com.fabien.domain

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
