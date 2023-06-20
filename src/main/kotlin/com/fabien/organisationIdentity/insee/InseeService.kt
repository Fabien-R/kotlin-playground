package com.fabien.organisationIdentity.insee

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.fabien.MissingNationalIdOrSearchText
import com.fabien.OrganizationIdentityError
import com.fabien.organisationIdentity.Organization
import com.fabien.organisationIdentity.OrganizationIdentityService
import com.fabien.organisationIdentity.PaginatedOrganizations
import com.fabien.organisationIdentity.insee.InseeQueryFields.*

fun inseeService(inseeApi: InseeApi) = object : OrganizationIdentityService {

    override suspend fun fetchIdentities(
        nationalId: String?,
        searchText: String?,
        zipCode: String?,
        pageSize: Int,
        page: Int,
    ): Either<OrganizationIdentityError, PaginatedOrganizations> =
        either {
            ensure(!nationalId.isNullOrEmpty() || !searchText.isNullOrEmpty()) { MissingNationalIdOrSearchText }
            formatToInseeParams(nationalId, searchText, zipCode, pageSize, page).let { inseeParams ->
                inseeApi.fetchInseeSuppliersSearch(inseeParams).bind().let { successfulResponse ->
                    // TODO if page > total * size -> header.nombre  = 0 -> should return outbound
                    PaginatedOrganizations(
                        organizations = successfulResponse.etablissements.map(Etablissement::toOrganization),
                        page = successfulResponse.header.debut / successfulResponse.header.nombre,
                        total = successfulResponse.header.total,
                    )
                }
            }
        }

    fun mapToInseeSearch(siret: String?, denomination: String?, zipCode: String?): String {
        return query {
            or {
                if (siret != null) SIRET contains siret
                if (denomination != null) {
                    or {
                        listOf(
                            // ETAB_DENOMINATION, more complicated because historized data
                            USUAL_FIRST_NAME_LEGAL_UNIT,
                            NAME_LEGAL_UNIT,
                            DENOMINATION_LEGAL_UNIT,
                            USUAL_DENOMINATION_LEGAL_UNIT_1,
                            USUAL_DENOMINATION_LEGAL_UNIT_2,
                            USUAL_DENOMINATION_LEGAL_UNIT_3,
                            USAGE_NAME_LEGAL_UNIT,
                            FIRST_NAME_LEGAL_UNIT_1,
                            FIRST_NAME_LEGAL_UNIT_2,
                            FIRST_NAME_LEGAL_UNIT_3,
                            FIRST_NAME_LEGAL_UNIT_4,
                        ).forEach { it approximateSearch denomination }
                    }
                }
            }
            if (zipCode != null) ZIP_CODE eq (zipCode)
        }.build()
    }

    fun formatToInseeParams(nationalId: String?, searchText: String?, zipCode: String?, pageSize: Int, page: Int) = mapOf(
        "q" to mapToInseeSearch(nationalId, searchText, zipCode),
        "champs" to InseeQueryFields.values().joinToString(separator = ",") { it.field },
        "nombre" to pageSize.toString(),
        "debut" to (page * pageSize).toString(),
        "tri" to "siret",
    )
}

const val COUNTRY_FRANCE = "FRANCE"

fun Etablissement.toOrganization() = Organization(
    name = uniteLegale.getName(),
    nationalId = siret,
    active = isActive(),
    country = COUNTRY_FRANCE,
    zipCode = adresseEtablissement?.codePostalEtablissement,
    city = adresseEtablissement?.libelleCommuneEtablissement,
    address = adresseEtablissement.toLineString(),
)

const val NO_LEGAL_UNIT_NAME = "NO NAME"

internal fun LegalUnit?.getName() =
    if (this?.isNaturalPerson() == true) {
        listOfNotNull(prenomUsuelUniteLegale, nomUniteLegale).joinToString(separator = " ")
    } else {
        listOfNotNull(
            this?.denominationUniteLegale,
            this?.denominationUsuelle1UniteLegale,
            this?.denominationUsuelle2UniteLegale,
            this?.denominationUsuelle3UniteLegale,
        ).firstOrNull() ?: NO_LEGAL_UNIT_NAME
    }

internal fun Etablissement.isActive() =
    uniteLegale?.etatAdministratifUniteLegale == "A" &&
        !periodesEtablissement.isNullOrEmpty() && periodesEtablissement[0].etatAdministratifEtablissement == "A"

internal fun Address?.toLineString() = listOfNotNull(
    this?.numeroVoieEtablissement,
    this?.typeVoieEtablissement,
    this?.libelleVoieEtablissement,
).joinToString(separator = " ")
