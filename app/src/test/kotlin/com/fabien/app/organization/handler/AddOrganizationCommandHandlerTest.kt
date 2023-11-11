package com.fabien.app.organization.handler

import arrow.core.Either
import com.fabien.app.*
import com.fabien.app.organization.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.*
import kotlin.test.assertContentEquals
import kotlin.test.assertIs

@Suppress("ComplexRedundantLet")
class AddOrganizationCommandHandlerTest {

    private lateinit var organizationRepository: OrganizationRepository
    private lateinit var addOrganizationCommandHandler: AddOrganizationCommandHandler

    @BeforeEach
    fun setup() {
        organizationRepository = mockk<OrganizationRepository>(relaxed = true)
        addOrganizationCommandHandler = addOrganizationCommandHandler(organizationRepository)
    }

    private fun errors(): List<Arguments> {
        val name = "EAU DU GRAND LYON"
        val nationalId = "79936588700048"
        val country = "FRANCE"
        return listOf(
            // message, name, nationalId, country, expected error
            Arguments.of("empty name", "", nationalId, country, listOf(MissingName)), //
            Arguments.of("null name", null, nationalId, country, listOf(MissingName)), //
            Arguments.of("empty nationalId", name, "", country, listOf(MissingNationalId)), //
            Arguments.of("null nationalId", name, null, country, listOf(MissingNationalId)), //
            Arguments.of("empty country", name, nationalId, "", listOf(MissingCountry)), //
            Arguments.of("null country", name, nationalId, null, listOf(MissingCountry)), //
            Arguments.of("no name, no nationalID", null, null, country, listOf(MissingName, MissingNationalId)), //
            Arguments.of("no name, no country", null, nationalId, null, listOf(MissingName, MissingCountry)), //
            Arguments.of("no nationalId, no country", name, null, null, listOf(MissingNationalId, MissingCountry)), //
            Arguments.of("no name, no nationalId, no country", null, null, null, listOf(MissingName, MissingNationalId, MissingCountry)), //
        )
    }

    @ParameterizedTest
    @MethodSource("errors")
    fun `when organization parameters is invalid throw the error`(
        message: String,
        name: String?,
        nationalId: String?,
        country: String?,
        expectedError: List<OrganizationCreationError>,
    ) = runTest {
        AddOrganizationCommand(name, nationalId, "69000", country, "LYON", null, true).let { cmd ->
            with(addOrganizationCommandHandler(cmd).leftOrNull()) {
                assertIs<OrganizationCreationErrorList>(this).let {
                    assertContentEquals(expectedError, it.errors, message)
                }
            }
        }
    }

    private fun addOrganizationMapper(): List<Arguments> {
        val name = "EAU DU GRAND LYON"
        val nationalId = "79936588700048"
        val zipCode = "69140"
        val country = "FRANCE"
        val city = "RILLIEUX-LA-PAPE"
        val address = "749 CHE DE VIRALAMANDE"
        val active = true
        return listOf(
            // message, name, nationalId, zipCode, country, city, address, active, expected organization
            Arguments.of(
                "uuid is hardcoded for now",
                name,
                nationalId,
                zipCode,
                country,
                city,
                address,
                active,
            ), //
            Arguments.of(
                "active false",
                name,
                nationalId,
                zipCode,
                country,
                city,
                address,
                false,
            ), //
            Arguments.of(
                "address null",
                name,
                nationalId,
                null,
                country,
                null,
                null,
                active,
            ), //
        )
    }

    @ParameterizedTest
    @MethodSource("addOrganizationMapper")
    fun `when receiving valid organization add command, should call the repository save with correct values`(
        message: String,
        name: String,
        nationalId: String,
        zipCode: String?,
        country: String,
        city: String?,
        address: String?,
        active: Boolean,
    ) = runTest {
        AddOrganizationCommand(name, nationalId, zipCode, country, city, address, active).let {
            with(addOrganizationCommandHandler(it).getOrNull()) {
                coVerify {
                    organizationRepository.save(eq(NewOrganization(name, nationalId, zipCode, country, city, address, active)))
                }
            }
        }
    }

    @Test
    fun `when receiving organization with null active attribute, should save it as active`() = runTest {
        val name = "EAU DU GRAND LYON"
        val nationalId = "79936588700048"
        val zipCode = "69140"
        val country = "FRANCE"
        val city = "RILLIEUX-LA-PAPE"
        val address = "749 CHE DE VIRALAMANDE"
        AddOrganizationCommand(name, nationalId, zipCode, country, city, address, null).let {
            with(addOrganizationCommandHandler(it).getOrNull()) {
                coVerify {
                    organizationRepository.save(eq(NewOrganization(name, nationalId, zipCode, country, city, address, true)))
                }
            }
        }
    }

    @Test
    fun `when saving a new organization, should return the repository response`() = runTest {
        val name = "EAU DU GRAND LYON"
        val nationalId = "79936588700048"
        val zipCode = "69140"
        val country = "FRANCE"
        val city = "RILLIEUX-LA-PAPE"
        val address = "749 CHE DE VIRALAMANDE"
        val active = true

        val newOrganization = NewOrganization(
            name = name,
            nationalId = nationalId,
            country = country,
            zipCode = zipCode,
            city = city,
            address = address,
            active = active,
        )

        val persistedOrganization = Organization(
            id = UUID.randomUUID(),
            name = name,
            nationalId = nationalId,
            country = country,
            zipCode = zipCode,
            city = city,
            address = address,
            active = active,
        )
        coEvery {
            organizationRepository.save(eq(newOrganization))
        } returns Either.Right(persistedOrganization)

        AddOrganizationCommand(name, nationalId, zipCode, country, city, address, active).let {
            with(addOrganizationCommandHandler(it).getOrNull()) {
                coVerify {
                    organizationRepository.save(eq(newOrganization))
                }
                assertEquals(persistedOrganization, this)
            }
        }
    }

    @Test
    fun `when saving a new organization, should return the error of the reposity if any`() = runTest {
        val name = "EAU DU GRAND LYON"
        val nationalId = "79936588700048"
        val zipCode = "69140"
        val country = "FRANCE"
        val city = "RILLIEUX-LA-PAPE"
        val address = "749 CHE DE VIRALAMANDE"
        val active = true

        val newOrganization = NewOrganization(
            name = name,
            nationalId = nationalId,
            country = country,
            zipCode = zipCode,
            city = city,
            address = address,
            active = active,
        )

        val repositoryError = OrganizationDuplication(
            country = country,
            nationalId = nationalId,
        )
        coEvery {
            organizationRepository.save(eq(newOrganization))
        } returns Either.Left(repositoryError)

        AddOrganizationCommand(name, nationalId, zipCode, country, city, address, active).let {
            with(addOrganizationCommandHandler(it).leftOrNull()) {
                coVerify {
                    organizationRepository.save(eq(newOrganization))
                }
                assertEquals(repositoryError, this)
            }
        }
    }
}
