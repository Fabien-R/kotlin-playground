package com.fabien.organisationIdentity.insee

import com.fabien.InseeException
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.auth.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import kotlinx.serialization.json.Json

const val TOKEN_VALIDITY_SECONDS = 10 * 1

// Trick other the ktor client plugin checks against the auth scheme Bearer see @io.ktor.client.plugins.auth.providers.BearerAuthProvider.isApplicable
class SimplifiedOAuth2BearerProvider(
    private val bearerAuthProvider: BearerAuthProvider,
) : AuthProvider by bearerAuthProvider {

    override fun isApplicable(auth: HttpAuthHeader): Boolean {
        return auth.authScheme == "${AuthScheme.OAuth}2"
    }
}

class InseeAuth(private val environment: ApplicationEnvironment) {
    private val bearerTokenStorage = mutableListOf<BearerTokens>()

    private val tokenClient = HttpClient(CIO.create()) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                },
            )
        }
    }

    val oAuth2 = SimplifiedOAuth2BearerProvider(
        BearerAuthProvider(
            refreshTokens = {
                tokenClient.queryTokenEndpoint { markAsRefreshTokenRequest() }
                    .also { response ->
                        if (!response.status.isSuccess()) {
                            throw InseeException(response.status)
                        }
                    }
                    .body<TokenInfo>()
                    .run { bearerTokenStorage.add(BearerTokens(this.accessToken, this.accessToken)) }
                bearerTokenStorage.last()
            },
            loadTokens = {
                tokenClient.queryTokenEndpoint()
                    .also { response ->
                        if (!response.status.isSuccess()) {
                            throw InseeException(response.status)
                        }
                    }.body<TokenInfo>()
                    .run { bearerTokenStorage.add(BearerTokens(this.accessToken, this.accessToken)) }
                bearerTokenStorage.last()
            },
            realm = null,
        ),
    )

    suspend fun HttpClient.queryTokenEndpoint(block: HttpRequestBuilder.() -> Unit = {}): HttpResponse =
        this.submitForm(
            formParameters = Parameters.build {
                append("grant_type", "client_credentials")
                append("validity_period", TOKEN_VALIDITY_SECONDS.toString())
            },
        ) {
            url {
                protocol = URLProtocol.HTTPS
                host = environment.config.property("insee.baseApi").getString()
                path(environment.config.property("insee.authenticationApi").getString())
            }
            headers {
                append(HttpHeaders.Authorization, "Basic ${environment.config.property("insee.base64ConsumerKeySecret").getString()}")
            }
            block()
        }
}
