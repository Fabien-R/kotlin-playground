package com.fabien.app.repository

import arrow.core.Either
import com.fabien.OrganizationsQueries
import com.fabien.domain.OrganizationDBError
import com.fabien.domain.OrganizationDBNotFound
import com.fabien.domain.OrganizationDuplication
import com.fabien.domain.OrganizationOtherDBErrors
import com.fabien.domain.model.NewOrganization
import com.fabien.domain.model.Organization
import com.fabien.domain.repositories.OrganizationRepository
import org.postgresql.util.PSQLException
import java.time.OffsetDateTime
import java.util.*

fun organizationRepository(
    queries: OrganizationsQueries,
) = object : OrganizationRepository {
    override suspend fun save(organization: NewOrganization): Either<OrganizationDBError, Organization> =
        Either.catch {
            queries.insertOrganization(
                name = organization.name,
                national_id = organization.nationalId,
                country = organization.country,
                zip_code = organization.zipCode,
                city = organization.city,
                address = organization.address,
                active = organization.active,
                mapper = toOrganization(),
            ).executeAsOne()
        }.mapLeft { dbException ->
            when {
                dbException is PSQLException &&
                    dbException.message != null &&
                    dbException.message!!.contains("duplicate key value violates unique constraint") ->
                    OrganizationDuplication(
                        organization.country,
                        organization.nationalId,
                    )

                else -> OrganizationOtherDBErrors(dbException.message)
            }
        }

    override suspend fun get(id: UUID): Either<OrganizationDBError, Organization> =
        Either.catch {
            queries.getOrganizationFromUUID(
                id = id,
                mapper = toOrganization(),
            ).executeAsOne()
        }.mapLeft { dbException ->
            when {
                dbException is NullPointerException -> OrganizationDBNotFound(id)
                else -> OrganizationOtherDBErrors(dbException.message)
            }
        }
}

fun toOrganization(): (
    UUID,
    String,
    String,
    String,
    String?,
    String?,
    String?,
    Boolean,
    OffsetDateTime,
    OffsetDateTime,
) -> Organization = {
        id,
        name,
        nationalId,
        country,
        zipCode,
        city,
        address,
        active,
        createdAt,
        updateAt,
    ->
    Organization(
        id = id,
        name = name,
        nationalId = nationalId,
        country = country,
        zipCode = zipCode,
        city = city,
        address = address,
        active = active,
    )
}
