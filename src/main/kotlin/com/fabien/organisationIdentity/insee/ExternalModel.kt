package com.fabien.organisationIdentity.insee

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TokenInfo(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Int,
    val scope: String,
    @SerialName("token_type") val tokenType: String,
)

@Serializable
data class InseeFaultyResponseHeader(
    val statut: Int,
    val message: String,
)

@Serializable
data class InseeFaultyResponse(
    val header: InseeFaultyResponseHeader,
)

@Serializable
data class InseeResponseHeader(
    val total: Int,
    val debut: Int,
    val nombre: Int,
)

@Serializable
data class LegalUnit(
    val categorieJuridiqueUniteLegale: Int,
    val prenomUsuelUniteLegale: String?,
    val nomUniteLegale: String?,
    val denominationUniteLegale: String?,
    val denominationUsuelle1UniteLegale: String?,
    val denominationUsuelle2UniteLegale: String?,
    val denominationUsuelle3UniteLegale: String?,
    val etatAdministratifUniteLegale: String?,
)

@Serializable
data class Address(
    val codePostalEtablissement: String,
    val libelleCommuneEtablissement: String,
    val numeroVoieEtablissement: String?,
    val typeVoieEtablissement: String?,
    val libelleVoieEtablissement: String?,
)

@Serializable
data class Etablissement(
    val uniteLegale: LegalUnit?,
    val siret: String,
    val adresseEtablissement: Address?,
)

@Serializable
data class InseeFault(
    val code: Int,
    val message: String,
    val description: String,
)

@Serializable
data class InseeResponse(
    val header: InseeResponseHeader? = null,
    val etablissements: List<Etablissement>? = null,
    val fault: InseeFault? = null
)