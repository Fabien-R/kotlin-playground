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