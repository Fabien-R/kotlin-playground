package com.fabien.organisationIdentity.insee

import kotlinx.serialization.Serializable

@Serializable
data class Organization(
    val name: String,
    val nationalId: String,
    val zipCode: String?,
    val country: String,
    val city: String?,
    val address: String,
    val active: Boolean,
)

@Serializable
data class PaginatedOrganizations(
    val organizations: List<Organization>,
    val page: Int,
    val total: Int,
)
