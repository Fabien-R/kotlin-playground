package com.fabien.repositories

import com.fabien.OrganizationsQueries
import com.fabien.domain.OrganizationDBNotFound
import com.fabien.domain.OrganizationDuplication
import com.fabien.domain.OrganizationOtherDBErrors
import com.fabien.domain.model.NewOrganization
import com.fabien.domain.repositories.OrganizationRepository
import com.fabien.repositories.containers.PostgresContainerIT
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.TimeoutException
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class OrganizationRepositoryTest {

    private val postgresContainerIT = PostgresContainerIT()
    private val organizationRepository: OrganizationRepository

    init {
        val database = database(postgresContainerIT.hikari)
        organizationRepository = organizationRepository(database.organizationsQueries)
    }

    @Test
    fun `when saving a new organization should return the organization`() = runTest {
        val name = "EAU DU GRAND LYON"
        val nationalId = "79936588700048"
        val country = "FRANCE"
        val zipCode = "69140"
        val city = "RILLIEUX-LA-PAPE"
        val address = "749 CHE DE VIRALAMANDE"
        val active = true
        organizationRepository.save(
            NewOrganization(
                name = name,
                nationalId = nationalId,
                country = country,
                zipCode = zipCode,
                city = city,
                address = address,
                active = active,
            ),
        ).getOrNull()!!.let {
            assertEquals(name, it.name)
            assertEquals(nationalId, it.nationalId)
            assertEquals(country, it.country)
            assertEquals(zipCode, it.zipCode)
            assertEquals(city, it.city)
            assertEquals(address, it.address)
            assertEquals(active, it.active)
            assertNotNull(it.id)
        }
    }

    @Test
    fun `after saving a new organization, should retrieve it via its id`() = runTest {
        val name = "EFG"
        val nationalId = "55208131766522"
        val country = "FRANCE"
        val zipCode = "75008"
        val city = "PARIS"
        val address = "22 AV DE WAGRAM"
        val active = true
        organizationRepository.save(
            NewOrganization(
                name = name,
                nationalId = nationalId,
                country = country,
                zipCode = zipCode,
                city = city,
                address = address,
                active = active,
            ),
        ).getOrNull()!!.let { savedOrganization ->
            with(organizationRepository.get(savedOrganization.id)) {
                assertEquals(savedOrganization, this.getOrNull())
            }
        }
    }

    @Test
    fun `if not find, should retrieve null`() = runTest {
        with(organizationRepository.get(UUID.randomUUID())) {
            assertIs<OrganizationDBNotFound>(this.leftOrNull())
        }
    }

    @Test
    fun `when the query throws an exception, should return generic DB error`() = runTest {
        val mock = mockk<OrganizationsQueries>()

        every {
            mock.getOrganizationFromUUID(UUID.randomUUID())
        } throws TimeoutException()

        with(organizationRepository(mock).get(UUID.randomUUID())) {
            assertIs<OrganizationOtherDBErrors>(this.leftOrNull())
        }
    }

    @Test
    fun `when saving a new organization with the same national id and country, should throw an exception`() = runTest {
        val name = "ENGIE"
        val nationalId = "54210765113030"
        val country = "FRANCE"
        val zipCode = "92400"
        val city = "COURBEVOIE"
        val address = "1 PL SAMUEL DE CHAMPLAIN"
        val active = true
        organizationRepository.save(
            NewOrganization(
                name = name,
                nationalId = nationalId,
                country = country,
                zipCode = zipCode,
                city = city,
                address = address,
                active = active,
            ),
        )

        organizationRepository.save(
            NewOrganization(
                name = name,
                nationalId = nationalId,
                country = country,
                zipCode = zipCode,
                city = city,
                address = address,
                active = active,
            ),
        ).leftOrNull().let {
            assertIs<OrganizationDuplication>(it)
        }
    }
}
