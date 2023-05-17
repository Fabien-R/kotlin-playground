package com.fabien.organisationIdentity.insee

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.fabien.InseeError
import com.fabien.InseeException
import com.fabien.InseeNotFound
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
                        // when there is no matching etab, Insee returns 404 but with empty message
                        if (it.status == HttpStatusCode.NotFound && it.status.description.isEmpty()) {
                            InseeNotFound
                        } else {
                            InseeError(it.status)
                        }
                    }
                }.let {
                    val body = it.body<InseeResponse>()
                    ensure(body.etablissements != null && body.header != null) { InseeError(HttpStatusCode(body.fault!!.code, body.fault.message)) }
                    SucessfullInseeResponse(body.header, body.etablissements)
                }
            }.mapLeft { inseeTokenException ->
                // Client authentication is handled via ktor plugins. We can not wrap their response with arrow type error handling framework before here
                InseeError(inseeTokenException.status)
                // FIXME should treat insee error when querying the token at a finer granularity and oustide the error of the fetch
                // we have a not found when the token api is wrong which is different of Not found for the fetch case when no etab is found...
            }.bind()
        }
}
