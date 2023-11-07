package com.fabien.app.organization.handler

import com.fabien.app.MissingCountry
import com.fabien.app.MissingName
import com.fabien.app.MissingNationalId
import com.fabien.app.OrganizationCreationError
import com.fabien.app.organization.AddOrganizationCommand
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
    // TODO test mapper
    // Test uuid
//    private fun mapper(): List<Arguments> {
//        val name = "EAU DU GRAND LYON"
//        val nationalId = "79936588700048"
//        val zipCode = "69140"
//        val country = "FRANCE"
//        val city = "RILLIEUX-LA-PAPE"
//        val address = "749 CHE DE VIRALAMANDE"
//        val active = true
//        return listOf(
//            // message, condition, expected
//            Arguments.of(), //
//        )
//    }

//    @ParameterizedTest
//    @MethodSource("comparisonConditions")
//    fun `Test comparison condition formatting`(message: String, condition: ComparisonCondition, expected: String) {
//        assertEquals(expected, condition.toString(), message)
//    }
}
