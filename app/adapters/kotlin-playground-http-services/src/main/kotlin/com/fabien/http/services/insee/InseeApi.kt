package com.fabien.http.services.insee

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.fabien.domain.InseeError
import com.fabien.domain.InseeException
import com.fabien.domain.InseeNotFound
import com.fabien.domain.InseeOtherError
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.serialization.kotlinx.xml.*
import kotlinx.serialization.json.Json

open class InseeApi(
    httpClientEngine: HttpClientEngine,
    inseeAuth: AuthProvider,
    private val host: String,
    private val siretAPI: String,
) {
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
                        host = this@InseeApi.host
                        parameters.appendAll(parametersOf(params.mapValues { listOf(it.value) }))
                        path(siretAPI)
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
                                    InseeError(it.status.value, it.status.description)
                                }
                            } else {
                                // Insee API Manager return xml error
                                val body = it.body<Fault>()
                                InseeOtherError(it.status.value, body.description)
                            }
                        } ?: InseeOtherError(it.status.value, "Impossible to deserialize Insee error")
                    }
                }.body()
            }.mapLeft { inseeTokenException ->
                // Client authentication is handled via ktor plugins. We can not wrap their response with arrow type error handling framework before here
                InseeError(inseeTokenException.statusCode, inseeTokenException.description)
                // FIXME should treat insee error when querying the token at a finer granularity and oustide the error of the fetch
                // we have a not found when the token api is wrong which is different of Not found for the fetch case when no etab is found...
            }.bind()
        }
}


fun inseeApi(
    host: String,
    siretAPI: String,
    authenticationAPI: String,
    consumerKeySecret: String,
    tokenValiditySeconds: String
) =
    InseeApi(
        CIO.create {
            requestTimeout = 3000
            maxConnectionsCount = 20
            endpoint {
                maxConnectionsPerRoute = 4
                keepAliveTime = 5000
                connectTimeout = 4000
                connectAttempts = 1
            }
        },
        inseeAuth(
            inseeAuthLoadToken(
                host = host,
                authenticationAPI = authenticationAPI,
                consumerKeySecret = consumerKeySecret,
                tokenValiditySeconds = tokenValiditySeconds,
            )
        ),
        host,
        siretAPI,
    )
