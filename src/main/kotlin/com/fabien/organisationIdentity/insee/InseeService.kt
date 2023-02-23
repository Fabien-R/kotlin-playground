package com.fabien.organisationIdentity.insee

import io.ktor.client.call.*

class InseeService(private val inseeApi: InseeApi) {
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

    suspend fun fetchInseeSuppliers(): PaginatedOrganizations {
        val body = inseeApi.fetchInseeSuppliersSearch(emptyMap()).body<InseeResponse>()
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