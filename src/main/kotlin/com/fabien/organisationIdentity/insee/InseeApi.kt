package com.fabien.organisationIdentity.insee

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import kotlinx.serialization.json.Json

class InseeApi(private val environment: ApplicationEnvironment) {
    private val client = HttpClient(CIO) {
        engine {
            threadsCount = 20
            requestTimeout = 3000
            maxConnectionsCount = 20
            endpoint {
                maxConnectionsPerRoute = 4
                keepAliveTime = 5000
                connectTimeout = 4000
                connectAttempts = 1
            }
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.HEADERS
        }
        install(Auth) {
            providers.add(InseeAuth(environment).oAuth2) // TODO should be a companion object or factory or something injected
        }
        install(HttpRequestRetry) {

        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = true
            })
        }
    }

    suspend fun fetchInseeSuppliersSearch() {
        val response = client.get {
            url {
                protocol = URLProtocol.HTTPS
                host = environment.config.property("insee.baseApi").getString()
                path(environment.config.property("insee.siretApi").getString())
            }
            contentType(ContentType.Application.Json)
        }
    }
}