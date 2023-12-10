package com.fabien.app.organisationIdentity.insee

import com.fabien.app.env.Env
import com.fabien.app.env.dependencies
import com.fabien.app.env.loadConfiguration
import com.fabien.app.module
import com.fabien.domain.model.NewOrganization
import com.fabien.domain.model.PaginatedOrganizations
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
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OrganizationIdentityTest {

    @Test
    fun searchOrganizationWithoutNationalIdNorSearchText() = testApplication {
        parametrizeApplicationTest()
        client.get("/organization/extract").apply {
            assertEquals(HttpStatusCode.UnprocessableEntity, status)
            assertEquals("Require at least one of the nationalId or searchText parameters", bodyAsText())
        }
    }

    @Test
    fun noOrganizationFound() = testApplication {
        parametrizeApplicationTest()
        createClientWithJsonNegotiation().get("/organization/extract?searchText=plopi&zipCode=6666").apply {
            assertEquals(HttpStatusCode.NotFound, status)
            assertEquals(PaginatedOrganizations(emptyList(), 0, 0), body())
        }
    }

    @Test
    fun searchOrganizationWithSearchText() = testApplication {
        parametrizeApplicationTest()
        createClientWithJsonNegotiation().get("/organization/extract?searchText=touten action").apply {
            assertEquals(HttpStatusCode.OK, status)
            NewOrganization(
                name = "TOUTEN ACTION",
                nationalId = "33445719900019",
                zipCode = "45270",
                country = "FRANCE",
                city = "LADON",
                address = "RTE NATIONALE 60",
                active = true,
            ).let {
                assertEquals(PaginatedOrganizations(listOf(it), 0, 1), body())
            }
        }
    }

    @Test
    fun searchOrganizationWithNationalId() = testApplication {
        parametrizeApplicationTest()
        createClientWithJsonNegotiation().get("/organization/extract?nationalId=00792667800017").apply {
            assertEquals(HttpStatusCode.OK, status)
            NewOrganization(
                name = "COPROPRIETE FONTAINE",
                nationalId = "00792667800017",
                zipCode = "19100",
                country = "FRANCE",
                city = "BRIVE-LA-GAILLARDE",
                address = "15 RUE CHARLES FOURRIER",
                active = true,
            ).let {
                assertEquals(PaginatedOrganizations(listOf(it), 0, 1), body())
            }
        }
    }

    @Test
    fun searchOrganizationWithZipCodeAndSearchText() = testApplication {
        parametrizeApplicationTest()
        createClientWithJsonNegotiation().get("/organization/extract?zipCode=33800&searchText=plop").apply {
            assertEquals(HttpStatusCode.OK, status)
            NewOrganization(
                name = "PLOP",
                nationalId = "78870646300015",
                zipCode = "33800",
                country = "FRANCE",
                city = "BORDEAUX",
                address = "91 RUE CAMILLE SAUVAGEAU",
                active = false,
            ).let {
                assertEquals(PaginatedOrganizations(listOf(it), 0, 1), body())
            }
        }
    }

    @Test
    fun searchOrganizationWithZipCodeAndNationalId() = testApplication {
        parametrizeApplicationTest()
        createClientWithJsonNegotiation().get("/organization/extract?zipCode=69003&nationalId=82454312800022").apply {
            assertEquals(HttpStatusCode.OK, status)
            NewOrganization(
                name = "EDF HYDRO DEVELOPPEMENT",
                nationalId = "82454312800022",
                zipCode = "69003",
                country = "FRANCE",
                city = "LYON 3EME",
                address = "106 BD MARIUS VIVIER MERLE",
                active = false,
            ).let {
                assertEquals(PaginatedOrganizations(listOf(it), 0, 1), body())
            }
        }
    }

    @Test
    fun searchOrganizationWithZipCodeAndNationalIdAndSearchText() = testApplication {
        parametrizeApplicationTest()
        createClientWithJsonNegotiation().get("/organization/extract?zipCode=69003&searchText=auchan&nationalId=39406971000090").apply {
            assertEquals(HttpStatusCode.OK, status)
            body<PaginatedOrganizations>().let {
                assertEquals(5, it.organizations.size, "as size")
                assertEquals(4, it.organizations.filter { organization -> organization.name.lowercase().contains("auchan") }.size, "only 4 are auchan")
                val nationalIdSearched = it.organizations.filter { organization -> organization.nationalId.contains("39406971000090") }
                assertEquals(1, nationalIdSearched.size, "searchText and nationalId are cumulative")
                assertEquals("MINISTERE DE LA REGION WALLONNE", nationalIdSearched[0].name, "searchText and nationalId are cumulative")
            }
        }
    }

    @Test
    fun searchOrganizationWithDifferentPageSize() = testApplication {
        parametrizeApplicationTest()
        createClientWithJsonNegotiation().get("/organization/extract?searchText=auchan").apply {
            assertEquals(HttpStatusCode.OK, status)
            body<PaginatedOrganizations>().let {
                assertEquals(5, it.organizations.size, "as default page size")
            }
        }
        val newPageSize = 10
        createClientWithJsonNegotiation().get("/organization/extract?searchText=auchan&pageSize=$newPageSize").apply {
            assertEquals(HttpStatusCode.OK, status)
            body<PaginatedOrganizations>().let {
                assertEquals(newPageSize, it.organizations.size, "modified page size")
            }
        }
    }

    @Test
    fun searchOrganizationPagination() = testApplication {
        parametrizeApplicationTest()
        val page1 = createClientWithJsonNegotiation().get("/organization/extract?searchText=auchan&page=1").run {
            assertEquals(HttpStatusCode.OK, status)
            body<PaginatedOrganizations>()
        }

        val page2 = createClientWithJsonNegotiation().get("/organization/extract?searchText=auchan&page=2").run {
            assertEquals(HttpStatusCode.OK, status)
            body<PaginatedOrganizations>()
        }

        assertEquals(1, page1.page)
        assertEquals(2, page2.page)
        assertEquals(page1.total, page2.total, "total number of organization should not change")
        assertTrue(page1.organizations.intersect(page2.organizations.toSet()).isEmpty(), "page should have different organizations")
    }

    @Test
    fun inseeWrongSecretShouldReturnInternalServerErrorWihUnAuthorizedStatusEncapsulatedInMessage() = testApplication {
        with(loadConfiguration(ApplicationConfig("application.yaml"))) {
            parametrizeApplicationTest(this.copy(insee = this.insee.copy(base64ConsumerKeySecret = "wrongKeySecret")))
        }
        createClientWithJsonNegotiation().get("/organization/extract?nationalId=00792667800017").apply {
            assertEquals(HttpStatusCode.InternalServerError, status)
            assertContains(bodyAsText(), "401")
        }
    }

    @Test
    fun inseeWrongSiretAPIShouldReturnInternalServerErrorWihNotFound() = testApplication {
        with(loadConfiguration(ApplicationConfig("application.yaml"))) {
            parametrizeApplicationTest(this.copy(insee = this.insee.copy(siretApi = "wrongSiretApi")))
        }
        createClientWithJsonNegotiation().get("/organization/extract?nationalId=00792667800017").apply {
            assertEquals(HttpStatusCode.InternalServerError, status)
            assertContains(bodyAsText(), "404")
        }
    }
}

context(ApplicationTestBuilder)
private fun parametrizeApplicationTest(env: Env = loadConfiguration(ApplicationConfig("application.yaml"))) {
    application {
        val dependencies = dependencies(env.insee, env.jwt, env.mindee, env.postgres)
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
