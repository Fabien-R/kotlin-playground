package com.fabien.app.repository

import com.fabien.OrganizationsQueries
import com.fabien.app.organization.NewOrganization
import com.fabien.app.organization.Organization
import com.fabien.app.organization.OrganizationRepository
import java.time.OffsetDateTime
import java.util.*

fun organizationRepository(
    queries: OrganizationsQueries,
) = object : OrganizationRepository {
    // TODO handle duplication error via arrow domain error + other DB related error
    override fun save(organization: NewOrganization): Organization =
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

    override fun get(id: UUID): Organization? =
        queries.getOrganizationFromUUID(
            id = id,
            mapper = toOrganization(),
        ).executeAsOneOrNull()
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
//    OffsetDateTime,
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
//        updateAt,
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
