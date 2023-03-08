package com.fabien.organisationIdentity.insee

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import kotlinx.serialization.json.Json

class InseeApi(private val environment: ApplicationEnvironment, httpClientEngine: HttpClientEngine, inseeAuth: InseeAuth) {
    private val client = HttpClient(httpClientEngine) {
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.HEADERS
        }
        install(Auth) {
            providers.add(inseeAuth.oAuth2) // TODO should be a companion object or factory or something injected
        }
        install(HttpRequestRetry) {
        }
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    prettyPrint = true
                },
            )
        }
    }

    suspend fun fetchInseeSuppliersSearch(params: Map<String, String>) =
        client.get {
            url {
                protocol = URLProtocol.HTTPS
                host = environment.config.property("insee.baseApi").getString()
                parameters.appendAll(parametersOf(params.mapValues { listOf(it.value) }))
                path(environment.config.property("insee.siretApi").getString())
            }
            contentType(ContentType.Application.Json)
        }
}
