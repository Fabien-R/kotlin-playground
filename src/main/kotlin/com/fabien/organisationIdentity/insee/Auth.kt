package com.fabien.organisationIdentity.insee

import com.fabien.InseeException
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

fun interface InseeLoadToken {
    suspend fun loadToken(block: HttpRequestBuilder.() -> Unit): BearerTokens
}

fun inseeAuthLoadToken(host: String, authenticationAPI: String, consumerKeySecret: String, tokenValiditySeconds: String) = object : InseeLoadToken {
    val bearerTokenStorage = mutableListOf<BearerTokens>()

    val tokenClient = HttpClient(CIO.create()) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                },
            )
        }
    }

    suspend fun HttpClient.queryTokenEndpoint(block: HttpRequestBuilder.() -> Unit = {}): HttpResponse =
        this.submitForm(
            formParameters = Parameters.build {
                append("grant_type", "client_credentials")
                append("validity_period", tokenValiditySeconds)
            },
        ) {
            url {
                protocol = URLProtocol.HTTPS
                this.host = host
                path(authenticationAPI)
            }
            headers {
                append(HttpHeaders.Authorization, "Basic $consumerKeySecret")
            }
            block()
        }

    override suspend fun loadToken(block: HttpRequestBuilder.() -> Unit): BearerTokens {
        tokenClient.queryTokenEndpoint { block() }
            .also { response ->
                if (!response.status.isSuccess()) {
                    throw InseeException(response.status)
                }
            }.body<TokenInfo>()
            .run { bearerTokenStorage.add(BearerTokens(this.accessToken, this.accessToken)) }
        return bearerTokenStorage.last()
    }
}

fun inseeAuth(inseeLoadToken: InseeLoadToken): BearerAuthProvider = BearerAuthProvider(
    refreshTokens = {
        inseeLoadToken.loadToken { markAsRefreshTokenRequest() }
    },
    loadTokens = {
        inseeLoadToken.loadToken {}
    },
    realm = null,
)
