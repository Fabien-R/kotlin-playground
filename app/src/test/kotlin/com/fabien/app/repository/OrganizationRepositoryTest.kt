package com.fabien.app.repository

import app.cash.sqldelight.driver.jdbc.asJdbcDriver
import com.fabien.Database
import com.fabien.app.containers.PostgresContainerIT
import com.fabien.app.env.database
import com.fabien.app.env.hikari
import com.fabien.app.organization.NewOrganization
import com.fabien.app.organization.OrganizationRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.postgresql.util.PSQLException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class OrganizationRepositoryTest {

    private val postgresContainerIT = PostgresContainerIT()
    private val organizationRepository: OrganizationRepository

    init {
        val hikari = hikari(postgresContainerIT.env)
        val database = database(hikari)
        // create tables
        Database.Schema.create(hikari.asJdbcDriver())
        organizationRepository = organizationRepository(database.organizationsQueries)
    }

    @Test
    fun `when saving a new organization should return the organization`() {
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
        ).let {
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
    fun `after saving a new organization, should retrieve it via its id`() {
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
        ).let { savedOrganization ->
            with(organizationRepository.get(savedOrganization.id)) {
                assertEquals(savedOrganization, this)
            }
        }
    }

    @Test
    fun `when saving a new organization with the same national id and country, should throw an exception`() {
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
        assertThrows<PSQLException>("duplicate key value violates unique constraint \"organizations_country_national_id_key\"") {
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
        }
    }
}
