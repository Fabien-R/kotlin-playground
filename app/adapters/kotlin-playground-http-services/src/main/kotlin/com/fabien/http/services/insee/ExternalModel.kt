package com.fabien.http.services.insee

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

enum class InseeQueryFields(val field: String) {
    SIRET("siret"),
    ETAB_DENOMINATION("denominationUsuelleEtablissement"),
    SIMPLE_IDENTIFICATION("identificationSimplifieeEtablissement"),
    DENOMINATION_LEGAL_UNIT("denominationUniteLegale"),
    USUAL_DENOMINATION_LEGAL_UNIT_1("denominationUsuelle1UniteLegale"),
    USUAL_DENOMINATION_LEGAL_UNIT_2("denominationUsuelle2UniteLegale"),
    USUAL_DENOMINATION_LEGAL_UNIT_3("denominationUsuelle3UniteLegale"),
    STATUTORY_LEGAL_UNIT("categorieJuridiqueUniteLegale"),
    USUAL_FIRST_NAME_LEGAL_UNIT("prenomUsuelUniteLegale"),
    NAME_LEGAL_UNIT("nomUniteLegale"),

    USAGE_NAME_LEGAL_UNIT("nomUsageUniteLegale"),
    FIRST_NAME_LEGAL_UNIT_1("prenom1UniteLegale"),
    FIRST_NAME_LEGAL_UNIT_2("prenom2UniteLegale"),
    FIRST_NAME_LEGAL_UNIT_3("prenom3UniteLegale"),
    FIRST_NAME_LEGAL_UNIT_4("prenom4UniteLegale"),

    ZIP_CODE("codePostalEtablissement"),
}

@Serializable
data class TokenInfo(
    @SerialName("access_token") val accessToken: String,
//    @SerialName("expires_in") val expiresIn: Int,
//    val scope: String,
//    @SerialName("token_type") val tokenType: String,
)

@Serializable
data class InseeResponseHeader(
    val total: Int,
    val debut: Int,
    val nombre: Int,
)

const val NATURAL_PERSON_INSEE_CONST = 1000

@Serializable
data class LegalUnit(
    val categorieJuridiqueUniteLegale: Int, // https://www.insee.fr/fr/information/2028129
    val prenomUsuelUniteLegale: String?,
    val nomUniteLegale: String?,
    val denominationUniteLegale: String?,
    val denominationUsuelle1UniteLegale: String?,
    val denominationUsuelle2UniteLegale: String?,
    val denominationUsuelle3UniteLegale: String?,
    val etatAdministratifUniteLegale: String?,
) { fun isNaturalPerson() = categorieJuridiqueUniteLegale == NATURAL_PERSON_INSEE_CONST }

@Serializable
data class Address(
    val codePostalEtablissement: String,
    val libelleCommuneEtablissement: String,
    val numeroVoieEtablissement: String?,
    val typeVoieEtablissement: String?,
    val libelleVoieEtablissement: String?,
)

@Serializable
data class PeriodEtablissement(
    val etatAdministratifEtablissement: String?,
)

@Serializable
data class Etablissement(
    val uniteLegale: LegalUnit?,
    val siret: String,
    val adresseEtablissement: Address?,
    val periodesEtablissement: List<PeriodEtablissement>? = null,
)

@XmlSerialName(prefix = "", namespace = "http://wso2.org/apimanager", value = "fault")
@Serializable
data class Fault(
    @XmlElement
    @XmlSerialName(value = "code")
    val code: Int,
    @XmlElement
    @XmlSerialName(value = "message")
    val message: String,
    @XmlSerialName(value = "type")
    @XmlElement
    val type: String,
    @XmlElement
    @XmlSerialName(value = "description")
    val description: String,
)

@Serializable
data class SucessfullInseeResponse(
    val header: InseeResponseHeader,
    val etablissements: List<Etablissement>,
)
