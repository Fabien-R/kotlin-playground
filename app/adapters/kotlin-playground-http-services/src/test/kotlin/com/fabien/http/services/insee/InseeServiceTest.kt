package com.fabien.http.services.insee

import io.mockk.*
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockKExtension::class)
class InseeServiceTest {

    private fun activeEtabs(): List<Arguments> {
        val activeEtab = Etablissement(
            uniteLegale = LegalUnit(
                categorieJuridiqueUniteLegale = NATURAL_PERSON_INSEE_CONST,
                prenomUsuelUniteLegale = "Jean-Michel",
                nomUniteLegale = "Touf",
                denominationUniteLegale = "SAS Astro",
                denominationUsuelle1UniteLegale = null,
                denominationUsuelle2UniteLegale = null,
                denominationUsuelle3UniteLegale = null,
                etatAdministratifUniteLegale = "A",
            ),
            siret = "99970646300015",
            adresseEtablissement = null,
            periodesEtablissement = listOf(PeriodEtablissement(etatAdministratifEtablissement = "A")),
        )
        return listOf(
            // message, etab, expected
            Arguments.of("Legal unit state null", activeEtab.copy(uniteLegale = null), false), //
            Arguments.of("Legal unit state closed", activeEtab.copy(uniteLegale = activeEtab.uniteLegale!!.copy(etatAdministratifUniteLegale = "C")), false), //
            Arguments.of("Legal unit state null", activeEtab.copy(uniteLegale = activeEtab.uniteLegale!!.copy(etatAdministratifUniteLegale = null)), false), //
            Arguments.of("Legal unit state empty", activeEtab.copy(uniteLegale = activeEtab.uniteLegale!!.copy(etatAdministratifUniteLegale = "")), false), //
            Arguments.of("Legal unit state empty", activeEtab.copy(uniteLegale = activeEtab.uniteLegale!!.copy(etatAdministratifUniteLegale = "")), false), //
            Arguments.of("Etab period null", activeEtab.copy(periodesEtablissement = null), false), //
            Arguments.of("Etab period empty", activeEtab.copy(periodesEtablissement = emptyList()), false), //
            Arguments.of(
                "Etab last period state null",
                activeEtab.copy(periodesEtablissement = listOf(activeEtab.periodesEtablissement!![0].copy(null))),
                false,
            ), //
            Arguments.of(
                "Etab last period state empty",
                activeEtab.copy(periodesEtablissement = listOf(activeEtab.periodesEtablissement!![0].copy(""))),
                false,
            ), //
            Arguments.of(
                "Etab last period state closed",
                activeEtab.copy(periodesEtablissement = listOf(activeEtab.periodesEtablissement!![0].copy("C"))),
                false,
            ), //
            Arguments.of("Etab with active legal unit state and active last period state", activeEtab, true), //
        )
    }

    @ParameterizedTest
    @MethodSource("activeEtabs")
    fun `is etab active`(message: String, etab: Etablissement, expected: Boolean) {
        assertEquals(expected, etab.isActive(), message)
    }

    private fun names(): List<Arguments> {
        val naturalPersonLegalUnit = LegalUnit(
            categorieJuridiqueUniteLegale = NATURAL_PERSON_INSEE_CONST,
            prenomUsuelUniteLegale = "Peter",
            nomUniteLegale = "Crow",
            denominationUniteLegale = null,
            denominationUsuelle1UniteLegale = null,
            denominationUsuelle2UniteLegale = null,
            denominationUsuelle3UniteLegale = null,
            etatAdministratifUniteLegale = null,
        )

        val corporateLegalUnit = LegalUnit(
            categorieJuridiqueUniteLegale = 5720,
            prenomUsuelUniteLegale = null,
            nomUniteLegale = null,
            denominationUniteLegale = "denominationUniteLegale",
            denominationUsuelle1UniteLegale = "denominationUsuelle1UniteLegale",
            denominationUsuelle2UniteLegale = "denominationUsuelle2UniteLegale",
            denominationUsuelle3UniteLegale = "denominationUsuelle3UniteLegale",
            etatAdministratifUniteLegale = null,
        )
        return listOf(
            // message, Legal Unit, expected
            Arguments.of(
                "Natural person",
                naturalPersonLegalUnit,
                "${naturalPersonLegalUnit.prenomUsuelUniteLegale} ${naturalPersonLegalUnit.nomUniteLegale}",
            ), //
            Arguments.of("Corporate with denomination", corporateLegalUnit, corporateLegalUnit.denominationUniteLegale), //
            Arguments.of(
                "Corporate without legal unit denomination",
                corporateLegalUnit.copy(denominationUniteLegale = null),
                corporateLegalUnit.denominationUsuelle1UniteLegale,
            ), //
            Arguments.of(
                "Corporate without legal unit denomination nor usuelle 1",
                corporateLegalUnit.copy(denominationUniteLegale = null, denominationUsuelle1UniteLegale = null),
                corporateLegalUnit.denominationUsuelle2UniteLegale,
            ), //
            Arguments.of(
                "Corporate without legal unit denomination nor usuelle 1 nor 2",
                corporateLegalUnit.copy(denominationUniteLegale = null, denominationUsuelle1UniteLegale = null, denominationUsuelle2UniteLegale = null),
                corporateLegalUnit.denominationUsuelle3UniteLegale,
            ), //
            Arguments.of(
                "Corporate with none denomination",
                corporateLegalUnit.copy(
                    denominationUniteLegale = null,
                    denominationUsuelle1UniteLegale = null,
                    denominationUsuelle2UniteLegale = null,
                    denominationUsuelle3UniteLegale = null,
                ),
                NO_LEGAL_UNIT_NAME,
            ), //
            Arguments.of(
                "no Legal unit",
                null,
                NO_LEGAL_UNIT_NAME,
            ), //
        )
    }

    @ParameterizedTest
    @MethodSource("names")
    fun `compute unit legal name`(message: String, unit: LegalUnit?, expected: String) {
        assertEquals(expected, unit.getName(), message)
    }

    private fun addresses(): List<Arguments> {
        val baseAddress = Address(
            codePostalEtablissement = "69003",
            libelleCommuneEtablissement = "Lyon",
            numeroVoieEtablissement = "87",
            typeVoieEtablissement = "Rue",
            libelleVoieEtablissement = "Garibaldi",
        )
        return listOf(
            // message, Adress, expected
            Arguments.of(
                "Everything filled",
                baseAddress,
                "${baseAddress.numeroVoieEtablissement} ${baseAddress.typeVoieEtablissement} ${baseAddress.libelleVoieEtablissement}",
            ), //
            Arguments.of(
                "Without house number",
                baseAddress.copy(numeroVoieEtablissement = null),
                "${baseAddress.typeVoieEtablissement} ${baseAddress.libelleVoieEtablissement}",
            ), //
            Arguments.of(
                "Without house number nor street type",
                baseAddress.copy(numeroVoieEtablissement = null, typeVoieEtablissement = null),
                "${baseAddress.libelleVoieEtablissement}",
            ), //
            Arguments.of(
                "With nothing",
                baseAddress.copy(numeroVoieEtablissement = null, typeVoieEtablissement = null, libelleVoieEtablissement = null),
                "",
            ), //
            Arguments.of(
                "With null address",
                null,
                "",
            ), //
        )
    }

    @ParameterizedTest
    @MethodSource("addresses")
    fun `compute address`(message: String, address: Address?, expected: String) {
        assertEquals(expected, address.toLineString(), message)
    }

    @Test
    fun `transform an Insee etab to an organization`() {
        mockkStatic(LegalUnit?::getName)
        mockkStatic(Address?::toLineString)
        // Given
        val legalUnit = mockk<LegalUnit>(relaxed = true)
        val address = mockk<Address>(relaxed = true)

        val etab = Etablissement(
            uniteLegale = legalUnit,
            siret = "66670646300015",
            adresseEtablissement = address,
            periodesEtablissement = emptyList(),
        )
        val spy = spyk(etab) {
            every {
                address.codePostalEtablissement
            } returns "66666"

            every {
                address.libelleCommuneEtablissement
            } returns "Coroulacious"
        }

        // When mapping
        val organization = spy.toOrganization()

        // Then
        verify(exactly = 1) {
            legalUnit.getName()
            spy.isActive()
            address.toLineString()
        }

        assertEquals(etab.siret, organization.nationalId)
        assertEquals(COUNTRY_FRANCE, organization.country)
        assertEquals(address.codePostalEtablissement, organization.zipCode)
        assertEquals(address.libelleCommuneEtablissement, organization.city)
    }
}
