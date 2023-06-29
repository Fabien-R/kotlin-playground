package com.fabien.organisationIdentity

import arrow.core.Either
import com.fabien.OrganizationIdentityError
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

fun interface OrganizationIdentityService {
    suspend fun fetchIdentities(
        nationalId: String?,
        searchText: String?,
        zipCode: String?,
        pageSize: Int,
        page: Int,
    ): Either<OrganizationIdentityError, PaginatedOrganizations>
}
