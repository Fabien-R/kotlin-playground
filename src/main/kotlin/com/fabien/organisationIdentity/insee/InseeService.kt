package com.fabien.organisationIdentity.insee

import com.fabien.organisationIdentity.insee.InseeQueryFields.*
import io.ktor.client.call.*
import io.ktor.http.*

class InseeService(private val inseeApi: InseeApi) {

    fun mapToInseeSearch(siret: String?, denomination: String?, zipCode: String?): String {
        return query {
            or {
                if (siret != null) SIRET contains siret
                if (denomination != null)
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

    fun toOrganization(etab: Etablissement): Organization {
        val name =
            if (etab.uniteLegale?.categorieJuridiqueUniteLegale == 1000)
                listOfNotNull(etab.uniteLegale.prenomUsuelUniteLegale, etab.uniteLegale.nomUniteLegale).joinToString(separator = " ")
            else
                listOfNotNull(
                    etab.uniteLegale?.denominationUniteLegale,
                    etab.uniteLegale?.denominationUsuelle1UniteLegale,
                    etab.uniteLegale?.denominationUsuelle2UniteLegale,
                    etab.uniteLegale?.denominationUsuelle3UniteLegale,
                ).firstOrNull() ?: "NO NAME"
        val address = listOfNotNull(
            etab.adresseEtablissement?.numeroVoieEtablissement,
            etab.adresseEtablissement?.typeVoieEtablissement,
            etab.adresseEtablissement?.libelleVoieEtablissement,
        ).joinToString(separator = " ")
        // TODO
        val active = true

        return Organization(
            name = name,
            nationalId = etab.siret,
            active = active,
            country = "FRANCE",
            zipCode = etab.adresseEtablissement?.codePostalEtablissement,
            city = etab.adresseEtablissement?.libelleCommuneEtablissement,
            address = address
        )
    }

    suspend fun fetchInseeSuppliers(nationalId: String?, searchText: String?, zipCode: String?, pageSize: Int, page: Int): PaginatedOrganizations {

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
            val organizations = body.etablissements.map(::toOrganization)

            return PaginatedOrganizations(
                organizations = organizations,
                page = body.header.debut / body.header.nombre,
                total = body.header.total,
            )
        } else {
            throw InseeException(body.fault!!.code, body.fault.message)
        }

    }
}