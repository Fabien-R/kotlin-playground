package com.fabien.organisationIdentity.insee

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.fabien.InseeError
import com.fabien.InseeException
import com.fabien.InseeNotFound
import com.fabien.InseeOtherError
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.serialization.kotlinx.xml.*
import io.ktor.server.application.*
import kotlinx.serialization.json.Json

class InseeApi(private val environment: ApplicationEnvironment, httpClientEngine: HttpClientEngine, inseeAuth: AuthProvider) {
    private val client = HttpClient(httpClientEngine) {
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.HEADERS
        }
        install(Auth) {
            providers.add(inseeAuth)
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
            xml()
        }
    }

    suspend fun fetchInseeSuppliersSearch(params: Map<String, String>) =
        either {
            Either.catchOrThrow<InseeException, SucessfullInseeResponse> {
                client.get {
                    url {
                        protocol = URLProtocol.HTTPS
                        host = environment.config.property("insee.baseApi").getString()
                        parameters.appendAll(parametersOf(params.mapValues { listOf(it.value) }))
                        path(environment.config.property("insee.siretApi").getString())
                    }
                    contentType(ContentType.Application.Json)
                }.also {
                    ensure(it.status.isSuccess()) {
                        it.contentType()?.let { contentType ->
                            if (JsonContentTypeMatcher.contains(contentType)) {
                                // when there is no matching etab, Insee returns 404 but with empty message
                                if (it.status == HttpStatusCode.NotFound && it.status.description.isEmpty()) {
                                    InseeNotFound
                                } else {
                                    InseeError(it.status)
                                }
                            } else {
                                // Insee API Manager return xml error
                                val body = it.body<Fault>()
                                InseeOtherError(it.status, body.description)
                            }
                        } ?: InseeOtherError(it.status, "Impossible to deserialize Insee error")
                    }
                }.body()
            }.mapLeft { inseeTokenException ->
                // Client authentication is handled via ktor plugins. We can not wrap their response with arrow type error handling framework before here
                InseeError(inseeTokenException.status)
                // FIXME should treat insee error when querying the token at a finer granularity and oustide the error of the fetch
                // we have a not found when the token api is wrong which is different of Not found for the fetch case when no etab is found...
            }.bind()
        }
}
