package com.fabien.app.organization.handler

import com.fabien.app.MissingCountry
import com.fabien.app.MissingName
import com.fabien.app.MissingNationalId
import com.fabien.app.OrganizationCreationError
import com.fabien.app.organization.AddOrganizationCommand
import com.fabien.app.organization.Organization
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertContentEquals

class AddOrganizationCommandHandlerTest {

    private val addOrganizationCommandHandler = addOrganizationCommandHandler()

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
    fun `when Organization parameters is invalid throw the error`(
        message: String,
        name: String?,
        nationalId: String?,
        country: String?,
        expectedError: List<OrganizationCreationError>,
    ) {
        @Suppress("ComplexRedundantLet")
        AddOrganizationCommand(name, nationalId, "69000", country, "LYON", null, true).let {
            with(addOrganizationCommandHandler(it).leftOrNull()) {
                assertContentEquals(expectedError, this!!.errors, message)
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
                Organization(FAKE_UUID.toUUID(), name, nationalId, country, zipCode, city, address, active),
            ), //
            Arguments.of(
                "active null",
                name,
                nationalId,
                zipCode,
                country,
                city,
                address,
                null,
                Organization(FAKE_UUID.toUUID(), name, nationalId, country, zipCode, city, address, true),
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
                Organization(FAKE_UUID.toUUID(), name, nationalId, country, zipCode, city, address, false),
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
                Organization(FAKE_UUID.toUUID(), name, nationalId, country, null, null, null, active),
            ), //
        )
    }

    @ParameterizedTest
    @MethodSource("addOrganizationMapper")
    fun `when receiving valid organization add command, should return the organization`(
        message: String,
        name: String,
        nationalId: String,
        zipCode: String?,
        country: String,
        city: String?,
        address: String?,
        active: Boolean?,
        expected: Organization,
    ) {
        AddOrganizationCommand(name, nationalId, zipCode, country, city, address, active).let {
            with(addOrganizationCommandHandler(it).getOrNull()) {
                assertEquals(expected, this, message)
            }
        }
    }
}
