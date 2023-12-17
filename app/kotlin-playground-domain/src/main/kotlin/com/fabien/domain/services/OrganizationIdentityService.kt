package com.fabien.domain.services

import arrow.core.Either
import com.fabien.domain.OrganizationIdentityError
import com.fabien.domain.model.PaginatedOrganizations

interface OrganizationIdentityService {
    suspend fun fetchIdentities(
        nationalId: String?,
        searchText: String?,
        zipCode: String?,
        pageSize: Int,
        page: Int,
    ): Either<OrganizationIdentityError, PaginatedOrganizations>
}
