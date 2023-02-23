package com.fabien.organisationIdentity.insee

import kotlinx.serialization.Serializable

class InseeException(val statusCode: Int, override val message: String): Exception(message)

@Serializable
data class Organization(
    val name: String,
    val nationalId: String,
    val zipCode: String?,
    val country: String,
    val city: String?,
    val address: String,
    val active: Boolean
)

@Serializable
data class PaginatedOrganizations(
    val organizations: List<Organization>,
    val page: Int,
    val total: Int,
)