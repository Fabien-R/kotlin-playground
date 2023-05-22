package com.fabien.organisationIdentity.insee

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlinx.coroutines.delay
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OrganizationIdentityTest {

    @Test
    fun searchOrganizationWithoutNationalIdNorSearchText() = testApplication {
        client.get("/organization/search").apply {
            assertEquals(HttpStatusCode.UnprocessableEntity, status)
            assertEquals("Require at least one of the nationalId or searchText parameters", bodyAsText())
        }
    }

    @Test
    fun noOrganizationFound() = testApplication {
        createClientWithJsonNegotiation().get("/organization/search?searchText=plopi&zipCode=6666").apply {
            assertEquals(HttpStatusCode.NotFound, status)
            assertEquals(PaginatedOrganizations(emptyList(), 0, 0), body())
        }
    }

    @Test
    fun searchOrganizationWithSearchText() = testApplication {
        createClientWithJsonNegotiation().get("/organization/search?searchText=touten action").apply {
            assertEquals(HttpStatusCode.OK, status)
            Organization(
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
        createClientWithJsonNegotiation().get("/organization/search?nationalId=00792667800017").apply {
            assertEquals(HttpStatusCode.OK, status)
            Organization(
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
        createClientWithJsonNegotiation().get("/organization/search?zipCode=33800&searchText=plop").apply {
            assertEquals(HttpStatusCode.OK, status)
            Organization(
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
        createClientWithJsonNegotiation().get("/organization/search?zipCode=69003&nationalId=82454312800022").apply {
            assertEquals(HttpStatusCode.OK, status)
            Organization(
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
        createClientWithJsonNegotiation().get("/organization/search?zipCode=69003&searchText=auchan&nationalId=39406971000090").apply {
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
        createClientWithJsonNegotiation().get("/organization/search?searchText=auchan").apply {
            assertEquals(HttpStatusCode.OK, status)
            body<PaginatedOrganizations>().let {
                assertEquals(5, it.organizations.size, "as default page size")
            }
        }
        val newPageSize = 10
        createClientWithJsonNegotiation().get("/organization/search?searchText=auchan&pageSize=$newPageSize").apply {
            assertEquals(HttpStatusCode.OK, status)
            body<PaginatedOrganizations>().let {
                assertEquals(newPageSize, it.organizations.size, "modified page size")
            }
        }
    }

    @Test
    fun searchOrganizationPagination() = testApplication {
        val page1 = createClientWithJsonNegotiation().get("/organization/search?searchText=auchan&page=1").run {
            assertEquals(HttpStatusCode.OK, status)
            body<PaginatedOrganizations>()
        }

        val page2 = createClientWithJsonNegotiation().get("/organization/search?searchText=auchan&page=2").run {
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
        environment {
            config = ApplicationConfig("application.yaml").mergeWith(MapApplicationConfig("insee.base64ConsumerKeySecret" to "wrongKeySecret"))
        }
        createClientWithJsonNegotiation().get("/organization/search?nationalId=00792667800017").apply {
            assertEquals(HttpStatusCode.InternalServerError, status)
            assertContains(bodyAsText(), "401")
        }
    }

    @Test
    fun inseeWrongSiretAPIShouldReturnInternalServerErrorWihNotFound() = testApplication {
        environment {
            config = ApplicationConfig("application.yaml").mergeWith(MapApplicationConfig("insee.siretApi" to "wrongSiretApi"))
        }
        createClientWithJsonNegotiation().get("/organization/search?nationalId=00792667800017").apply {
            assertEquals(HttpStatusCode.InternalServerError, status)
            assertContains(bodyAsText(), "404")
        }
    }

    @Test
    fun inseeShouldRefreshToken() = testApplication {
        environment {
            // 1 second is not enough to make the query after loading the token
            config = ApplicationConfig("application.yaml").mergeWith(MapApplicationConfig("insee.tokenValiditySeconds" to "2"))
        }
        createClientWithJsonNegotiation().get("/organization/search?nationalId=00792667800017").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        // force the token to be expired
        delay(3000)

        createClientWithJsonNegotiation().get("/organization/search?nationalId=00792667800017").apply {
            assertEquals(HttpStatusCode.OK, status)
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