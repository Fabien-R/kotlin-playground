package com.fabien.app.organization

import com.fabien.app.env.Env
import com.fabien.app.env.dependencies
import com.fabien.app.env.loadConfiguration
import com.fabien.app.module
import com.fabien.app.organization.handler.FAKE_UUID
import com.fabien.app.organization.handler.toUUID
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class OrganizationTest {

    @Test
    fun `when adding an organization without national Id should throw matching error`() = testApplication {
        parametrizeApplicationTest()
        createClientWithJsonNegotiation().post("/organization") {
            setBody(
                AddOrganizationDTO(name = "Eau du grand Lyon", country = "FRANCE"),
            )
            contentType(ContentType.Application.Json)
        }.apply {
            assertEquals(HttpStatusCode.UnprocessableEntity, status)
            assertEquals("Errors: missing national id", bodyAsText())
        }
    }

    @Test
    fun `when adding an invalid organization should throw combine errors`() = testApplication {
        parametrizeApplicationTest()
        createClientWithJsonNegotiation().post("/organization") {
            setBody(
                AddOrganizationDTO(),
            )
            contentType(ContentType.Application.Json)
        }.apply {
            assertEquals(HttpStatusCode.UnprocessableEntity, status)
            assertEquals("Errors: missing name|missing national id|missing country", bodyAsText())
        }
    }

    @Test
    fun `when adding a valid organization should return it`() = testApplication {
        val name = "EAU DU GRAND LYON"
        val nationalId = "79936588700048"
        val zipCode = "69140"
        val country = "FRANCE"
        val city = "RILLIEUX-LA-PAPE"
        val address = "749 CHE DE VIRALAMANDE"
        val active = true
        parametrizeApplicationTest()
        createClientWithJsonNegotiation().post("/organization") {
            setBody(
                AddOrganizationDTO(
                    name = name,
                    nationalId = nationalId,
                    zipCode = zipCode,
                    country = country,
                    city = city,
                    address = address,
                    active = active,
                ),
            )
            contentType(ContentType.Application.Json)
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
            Organization(
                id = FAKE_UUID.toUUID(),
                name = name,
                nationalId = nationalId,
                zipCode = zipCode,
                country = country,
                city = city,
                address = address,
                active = active,
            ).let {
                assertEquals(it, body())
            }
        }
    }
}

context(ApplicationTestBuilder)
private fun parametrizeApplicationTest(env: Env = loadConfiguration(ApplicationConfig("application.yaml"))) {
    application {
        val dependencies = dependencies(env.insee, env.jwt, env.mindee)
        module(dependencies)
    }
}

context(ApplicationTestBuilder)
private fun createClientWithJsonNegotiation(): HttpClient =
    createClient {
        install(ContentNegotiation) {
            json()
        }
    }
