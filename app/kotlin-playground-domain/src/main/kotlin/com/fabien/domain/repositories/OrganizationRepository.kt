package com.fabien.domain.repositories

import arrow.core.Either
import com.fabien.domain.OrganizationDBError
import com.fabien.domain.model.NewOrganization
import com.fabien.domain.model.Organization
import java.util.*

interface OrganizationRepository {
    suspend fun save(organization: NewOrganization): Either<OrganizationDBError, Organization>

    suspend fun get(id: UUID): Either<OrganizationDBError, Organization?>
}
