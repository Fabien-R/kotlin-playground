package com.fabien.organisationIdentity.insee

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class InseeApiTest {
    @Before
    fun setUp() = MockKAnnotations.init(this, relaxUnitFun = true)

    @Test
    @Disabled("Did not manage to correctly mock extension function HttpClient#queryTokenEndpoint inside InseeAuth")
    fun `should call token API when 401`() = runBlocking {
        val mockEngine = MockEngine { request ->
            respond(
                content = "Token outdated",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val mockedEnv = mockk<ApplicationEnvironment> {
            every { config.property(eq("insee.baseApi")).getString() } returns "baseApi"
            every { config.property(eq("insee.siretApi")).getString() } returns "siretApi"
            every { config.property(eq("insee.authenticationApi")).getString() } returns "authApi"
            every { config.property(eq("insee.base64ConsumerKeySecret")).getString() } returns "base64ConsumerKeySecret"
        }

        mockkConstructor(HttpClient::class)

        val mockedAuth = spyk(InseeAuth(mockedEnv)) {
            coEvery { anyConstructed<HttpClient>().queryTokenEndpoint() } returns (
                mockk {
                    every { status } returns io.ktor.http.HttpStatusCode.BadRequest
                    mockkStatic(HttpClientCall::body)
                    coEvery { body<TokenInfo>() } returns TokenInfo("token", 888, "full", "type")
                }
                )
        }

        val inseeApi = InseeApi(mockedEnv, mockEngine, mockedAuth)

        inseeApi.fetchInseeSuppliersSearch(emptyMap())

        with(mockedAuth) {
            coVerify { anyConstructed<HttpClient>().queryTokenEndpoint() }
        }
    }
}
