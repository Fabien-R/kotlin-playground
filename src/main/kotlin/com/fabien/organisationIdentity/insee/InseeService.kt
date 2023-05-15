package com.fabien.organisationIdentity.insee

import com.fabien.organisationIdentity.insee.InseeQueryFields.*
import io.ktor.client.call.*
import io.ktor.http.*

interface InseeService {
    suspend fun fetchInseeSuppliers(nationalId: String?, searchText: String?, zipCode: String?, pageSize: Int, page: Int): PaginatedOrganizations
}

fun inseeService(inseeApi: InseeApi) = object : InseeService {

    override suspend fun fetchInseeSuppliers(nationalId: String?, searchText: String?, zipCode: String?, pageSize: Int, page: Int): PaginatedOrganizations {
        val response = inseeApi.fetchInseeSuppliersSearch(formatToInseeParams(nationalId, searchText, zipCode, pageSize, page))

        if (!response.status.isSuccess()) {
            val body = response.body<InseeFaultyResponse>()

            // when there is no matching etab, Insee returns 404
            if (body.header.statut == HttpStatusCode.NotFound.value && body.header.message.contains("Aucun élément trouvé")) {
                return PaginatedOrganizations(
                    organizations = emptyList(),
                    page = 0,
                    total = 0,
                )
            }
            throw InseeException(body.header.statut, body.header.message)
        }

        val body = response.body<InseeResponse>()
        if (body.etablissements != null && body.header != null) {
            val organizations = body.etablissements.map(Etablissement::toOrganization)

            return PaginatedOrganizations(
                organizations = organizations,
                page = body.header.debut / body.header.nombre,
                total = body.header.total,
            )
        } else {
            throw InseeException(body.fault!!.code, body.fault.message)
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
