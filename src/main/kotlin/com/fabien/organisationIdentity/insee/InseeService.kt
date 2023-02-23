package com.fabien.organisationIdentity.insee

import com.fabien.organisationIdentity.insee.InseeQueryFields.*
import io.ktor.client.call.*

class InseeService(private val inseeApi: InseeApi) {

    fun mapToInseeSearch(siret: String?, denomination: String?, zipCode: String?): String {
        return query {
            or {
                if (siret != null) SIRET contains siret
                if (denomination != null)
                    or {
                        // ETAB_DENOMINATION, more complicated because historized data
                        USUAL_FIRST_NAME_LEGAL_UNIT approximateSearch denomination
                        NAME_LEGAL_UNIT approximateSearch denomination
                        DENOMINATION_LEGAL_UNIT approximateSearch denomination
                        USUAL_DENOMINATION_LEGAL_UNIT_1 approximateSearch denomination
                        USUAL_DENOMINATION_LEGAL_UNIT_2 approximateSearch denomination
                        USUAL_DENOMINATION_LEGAL_UNIT_3 approximateSearch denomination
                        USAGE_NAME_LEGAL_UNIT approximateSearch denomination
                        FIRST_NAME_LEGAL_UNIT_1 approximateSearch denomination
                        FIRST_NAME_LEGAL_UNIT_2 approximateSearch denomination
                        FIRST_NAME_LEGAL_UNIT_3 approximateSearch denomination
                        FIRST_NAME_LEGAL_UNIT_4 approximateSearch denomination
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

        val body = inseeApi.fetchInseeSuppliersSearch(formatToInseeParams(nationalId, searchText, zipCode, pageSize, page)).body<InseeResponse>()
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