package com.fabien.organisationIdentity.insee

import com.fabien.authent.REGISTERED_AUTHORIZATION
import com.fabien.authent.STAFF_AUTHORIZATION
import com.fabien.env.Env
import com.fabien.env.dependencies
import com.fabien.env.loadConfiguration
import com.fabien.module
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.config.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

const val WRONG_TOKEN = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6Ik9UZEVSRVl5T0RFM09EVTVOVVE1UlVFeFJUaERRVU5CUXpBd016UXlSRFpDTmpBeFFqQkdNQSJ9.eyJodHRwczovL2hhc3VyYS5pby9qd3QvY2xhaW1zIjp7IngtaGFzdXJhLWFsbG93ZWQtcm9sZXMiOlsidXNlciIsImFkbWl1Il0sIngtaGFzdXJhLWRlZmF1bHQtcm9sZSI6ImFkbWluIiwieC1oYXN1cmEtdXNlci1pZCI6IjYzMDEwNjA2LTBmMTctNDVlOS1hYzFiLWQyYTIwNDRlMmQ4YSJ9LCJ1c2VySWQiOiI2MzAxMDYwNi0wZjE3LTQ1ZTktYWMxYi1kMmEyMDQ0ZTJkOGEiLCJpc3MiOiJodHRwczovL2FnYXBpby1kZXYuZXUuYXV0aDAuY29tLyIsInN1YiI6ImF1dGgwfDYxNDFlNDNmYTc4YzljMDA3MDYwMzkxZiIsImF1ZCI6WyJodHRwOi8vbG9jYWxob3N0OjgwODAiLCJodHRwczovL2FnYXBpby1kZXYuZXUuYXV0aDAuY29tL3VzZXJpbmZvIl0sImlhdCI6MTY4NTAyMTQ4MCwiZXhwIjoxNjg1MTA3ODgwLCJhenAiOiJMblZKTDcwd1VmZzJFMkh4VTZnSEJvUWdVOVVtRTdkRCIsInNjb3BlIjoib3BlbmlkIHByb2ZpbGUgZW1haWwiLCJndHkiOiJwYXNzd29yZCIsInBlcm1pc3Npb25zIjpbIkJPOio6d3JpdGUiLCJjbGllbnQ6YWNjb3VudGluZzpyZWFkIiwiY2xpZW50OmRhc2hib2FyZHM6cmVhZCJdfQ.FSBXuX31IVjRuwInDrnn4208QFuBJjQAe47PRixTarKAN8QaJQJLyYs1KPnNx-msteeFK8vN6yG9mWvk5xpTxSFNzav1aaZMt2J4mfELl2udr10WQ3Z-YfV5fmNufvWjGuE0ZrYvdA95xlUe_qNhbURRUKkym9FYTl81gt89shHDgykgMJRiMoCXoLHcfIIG4Uw3CxgEcwFjaSd0u7lrJYZzOk59SjIL-4gQ2reZ1PY6djqWjg9aSJ7T9cJvlGiCNOrGyPATmtfNJT6KH_ZyCjQMhhKm8Z339qODbwSmUiOOUGjVNizuFjP7WS5X309o2FGMzb16BMxeCBgRmlulyQ"

class AuthenticationAndAuthorizationTest {

    @Test
    fun shouldNotAuthorizeNotRegisteredUser() = testApplication {
        parametrizeApplicationTest()
        createClientWithJsonNegotiation().get("/registered") {
            headers {
                append(HttpHeaders.Authorization, "Bearer $WRONG_TOKEN")
            }
        }.apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }

        createClientWithJsonNegotiation().get("/staff") {
            headers {
                append(HttpHeaders.Authorization, "Bearer $WRONG_TOKEN")
            }
        }.apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }
}

context(ApplicationTestBuilder)
private fun parametrizeApplicationTest(env: Env = loadConfiguration(ApplicationConfig("application.yaml"))) {
    application {
        val dependencies = dependencies(env.insee, env.jwt)
        module(dependencies)
        // DUMMY
        routing {
            authenticate(REGISTERED_AUTHORIZATION) {
                get("/registered") {
                    val userId = call.principal<JWTPrincipal>()?.getClaim("userId", String::class)
                    call.respondText { "Hey $userId" }
                }
            }
            authenticate(STAFF_AUTHORIZATION) {
                get("/staff") {
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
