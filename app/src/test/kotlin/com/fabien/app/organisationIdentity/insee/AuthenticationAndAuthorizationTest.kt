package com.fabien.app.organisationIdentity.insee

import com.fabien.app.authent.REGISTERED_AUTHORIZATION
import com.fabien.app.authent.STAFF_AUTHORIZATION
import com.fabien.app.env.Env
import com.fabien.app.env.Jwt
import com.fabien.app.env.dependencies
import com.fabien.app.env.loadConfiguration
import com.fabien.app.module
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.http.ContentType.Application.FormUrlEncoded
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.config.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import kotlin.test.assertEquals

const val WRONG_TOKEN =
    "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6Ik9UZEVSRVl5T0RFM09EVTVOVVE1UlVFeFJUaERRVU5CUXpBd016UXlSRFpDTmpBeFFqQkdNQSJ9.eyJodHRwczovL2hhc3VyYS5pby9qd3QvY2xhaW1zIjp7IngtaGFzdXJhLWFsbG93ZWQtcm9sZXMiOlsidXNlciIsImFkbWl1Il0sIngtaGFzdXJhLWRlZmF1bHQtcm9sZSI6ImFkbWluIiwieC1oYXN1cmEtdXNlci1pZCI6IjYzMDEwNjA2LTBmMTctNDVlOS1hYzFiLWQyYTIwNDRlMmQ4YSJ9LCJ1c2VySWQiOiI2MzAxMDYwNi0wZjE3LTQ1ZTktYWMxYi1kMmEyMDQ0ZTJkOGEiLCJpc3MiOiJodHRwczovL2FnYXBpby1kZXYuZXUuYXV0aDAuY29tLyIsInN1YiI6ImF1dGgwfDYxNDFlNDNmYTc4YzljMDA3MDYwMzkxZiIsImF1ZCI6WyJodHRwOi8vbG9jYWxob3N0OjgwODAiLCJodHRwczovL2FnYXBpby1kZXYuZXUuYXV0aDAuY29tL3VzZXJpbmZvIl0sImlhdCI6MTY4NTAyMTQ4MCwiZXhwIjoxNjg1MTA3ODgwLCJhenAiOiJMblZKTDcwd1VmZzJFMkh4VTZnSEJvUWdVOVVtRTdkRCIsInNjb3BlIjoib3BlbmlkIHByb2ZpbGUgZW1haWwiLCJndHkiOiJwYXNzd29yZCIsInBlcm1pc3Npb25zIjpbIkJPOio6d3JpdGUiLCJjbGllbnQ6YWNjb3VudGluZzpyZWFkIiwiY2xpZW50OmRhc2hib2FyZHM6cmVhZCJdfQ.FSBXuX31IVjRuwInDrnn4208QFuBJjQAe47PRixTarKAN8QaJQJLyYs1KPnNx-msteeFK8vN6yG9mWvk5xpTxSFNzav1aaZMt2J4mfELl2udr10WQ3Z-YfV5fmNufvWjGuE0ZrYvdA95xlUe_qNhbURRUKkym9FYTl81gt89shHDgykgMJRiMoCXoLHcfIIG4Uw3CxgEcwFjaSd0u7lrJYZzOk59SjIL-4gQ2reZ1PY6djqWjg9aSJ7T9cJvlGiCNOrGyPATmtfNJT6KH_ZyCjQMhhKm8Z339qODbwSmUiOOUGjVNizuFjP7WS5X309o2FGMzb16BMxeCBgRmlulyQ"

enum class Endpoints(val url: String) {
    REGISTERED("/registered"),
    STAFF("/staff"),
}

enum class Users(val email: String, val password: String) {
    STAFF(email = System.getenv("STAFF_MAIL"), password = System.getenv("STAFF_PASSWORD")),
    REGISTERED(email = System.getenv("USER_MAIL"), password = System.getenv("USER_PASSWORD")),
}
class AuthenticationAndAuthorizationTest {

    @ParameterizedTest
    @EnumSource(Endpoints::class)
    fun shouldNotAuthorizeNotRegisteredUserOnAuthenticatedEndPoints(endpoint: Endpoints) = testApplication {
        loadConfiguration(ApplicationConfig("application.yaml")).let { env ->
            parametrizeApplicationTest(env)
            createClientWithJsonNegotiation().get(endpoint.url) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $WRONG_TOKEN")
                }
            }.apply {
                assertEquals(HttpStatusCode.Unauthorized, status)
            }
        }
    }

    @ParameterizedTest
    @EnumSource(Users::class)
    fun shouldAuthorizeRegisteredUserOnAuthenticatedEndPoints(user: Users) = testApplication {
        loadConfiguration(ApplicationConfig("application.yaml")).let { env ->
            parametrizeApplicationTest(env)
            getBearer(env.jwt, user).let { bearer ->
                createClientWithJsonNegotiation().get(Endpoints.REGISTERED.url) {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $bearer")
                    }
                }.apply {
                    assertEquals(HttpStatusCode.OK, status)
                }
            }
        }
    }

    @Test
    fun shouldNotAuthorizeRegisteredUserOnStaffEndPoints() = testApplication {
        loadConfiguration(ApplicationConfig("application.yaml")).let { env ->
            parametrizeApplicationTest(env)
            getBearer(env.jwt, Users.REGISTERED).let { bearer ->
                createClientWithJsonNegotiation().get(Endpoints.STAFF.url) {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $bearer")
                    }
                }.apply {
                    assertEquals(HttpStatusCode.Unauthorized, status)
                }
            }
        }
    }

    @Test
    fun shouldAuthorizeStaffUserOnStaffEndPoints() = testApplication {
        loadConfiguration(ApplicationConfig("application.yaml")).let { env ->
            parametrizeApplicationTest(env)
            getBearer(env.jwt, Users.STAFF).let { bearer ->
                createClientWithJsonNegotiation().get(Endpoints.STAFF.url) {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $bearer")
                    }
                }.apply {
                    assertEquals(HttpStatusCode.OK, status)
                }
            }
        }
    }
}

context(ApplicationTestBuilder)
private fun parametrizeApplicationTest(env: Env) {
    application {
        val dependencies = dependencies(env.insee, env.jwt, env.mindee, env.postgres)
        module(dependencies)
        // DUMMY
        routing {
            authenticate(REGISTERED_AUTHORIZATION) {
                get(Endpoints.REGISTERED.url) {
                    val userId = call.principal<JWTPrincipal>()?.getClaim("userId", String::class)
                    call.respondText { "Hey $userId" }
                }
            }
            authenticate(STAFF_AUTHORIZATION) {
                get(Endpoints.STAFF.url) {
                    val userId = call.principal<JWTPrincipal>()?.getClaim("userId", String::class)
                    call.respondText { "Hey $userId" }
                }
            }
        }
    }
}

context(ApplicationTestBuilder)
private fun createClientWithJsonNegotiation(): HttpClient =
    createClient {
        install(ContentNegotiation) {
            json()
        }
    }

private suspend fun getBearer(config: Jwt, user: Users): String =
    HttpClient {
        install(ContentNegotiation) {
            json()
        }
    }.post("${config.domain}oauth/token") {
        setBody(
            FormDataContent(
                Parameters.build {
                    append("audience", config.audience)
                    append("grant_type", "password")
                    append("username", user.email)
                    append("password", user.password)
                    append("client_id", System.getenv("JWT_CLIENT_ID"))
                    append("client_secret", System.getenv("JWT_CLIENT_SECRET"))
                },
            ),
        )
        contentType(FormUrlEncoded)
    }.body<Auth0Token>().access_token

@Serializable
data class Auth0Token(val access_token: String, val expires_in: Int, val token_type: String)
