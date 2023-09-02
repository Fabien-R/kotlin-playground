package com.fabien.app.organisationIdentity.insee

import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.http.*
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.jupiter.api.Test

internal class InseeApiTest {
    @Before
    fun setUp() = MockKAnnotations.init(this, relaxUnitFun = true)

    @Test
    fun `should refresh Insee token API when 401`() = runBlocking {
        val mock = mockk<InseeLoadToken> {
            coEvery {
                loadToken(any())
            } returns BearerTokens("accessToken", "refreshToken")
        }
        val mockEngine = MockEngine {
            respond(
                content = "Token outdated",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val inseeApi = InseeApi(mockEngine, inseeAuth(mock), "baseAPI", "siretAPI")

        inseeApi.fetchInseeSuppliersSearch(emptyMap())

        with(mock) {
            coVerify { loadToken(any()) }
        }
    }
}
