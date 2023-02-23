package com.fabien.organisationIdentity.insee

import com.fabien.organisationIdentity.insee.InseeQueryFields.*
import io.ktor.client.call.*

class InseeService(private val inseeApi: InseeApi) {
    fun mapToInseeSearch(siret: String?, denomination: String?, zipCode: String?): String {
        val siretFilter = if (siret != null) "siret:*$siret*" else null
        val denominationPattern = if (denomination != null) "\"$denomination\"~2" else null
        val denominationFilter = if (denominationPattern != null) arrayOf(
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
        ).joinToString(separator = " OR ") { "${it.field}:$denominationPattern" }
        else null

        val zipCodeFilter = if (zipCode != null) "codePostalEtablissement:$zipCode" else null
// TODO could a DSL make it more generic? is it possible?
        return arrayOf(
            arrayOf(siretFilter, denominationFilter)
                .filterNotNull().joinToString(separator = " OR ") { "($it)" },
            zipCodeFilter
        )
            .filterNotNull()
            .joinToString(separator = " AND ")
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