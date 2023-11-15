package com.fabien.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class NewOrganization(
    val name: String,
    val nationalId: String,
    val zipCode: String?,
    val country: String,
    val city: String?,
    val address: String?,
    val active: Boolean,
)

@Serializable
data class PaginatedOrganizations(
    val organizations: List<NewOrganization>,
    val page: Int,
    val total: Int,
)
