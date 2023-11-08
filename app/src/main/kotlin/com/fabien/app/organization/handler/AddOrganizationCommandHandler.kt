package com.fabien.app.organization.handler

import arrow.core.NonEmptyList
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate
import com.fabien.app.*
import com.fabien.app.organization.AddOrganizationCommandHandler
import com.fabien.app.organization.Organization
import java.lang.Boolean.FALSE
import java.util.*

const val FAKE_UUID = "3068da80-d903-4ad4-bfc6-ce3a5123b88"

fun String.toUUID(): UUID = UUID.fromString(this)
fun addOrganizationCommandHandler() = AddOrganizationCommandHandler { addCommand ->
    val (name, nationalId, zipCode, country, city, address, active) = addCommand
    // TODO TRIM field name, zipcode, country, city, address
    // TODO remove space from nationalID
    either<NonEmptyList<OrganizationCreationError>, Organization> {
        zipOrAccumulate(
            { ensure(!name.isNullOrEmpty()) { MissingName } },
            { ensure(!nationalId.isNullOrEmpty()) { MissingNationalId } },
            { ensure(!country.isNullOrEmpty()) { MissingCountry } },
            // TODO add national Id check
        ) { _, _, _ ->
            // TODO Upper case country, city
            Organization(
                id = FAKE_UUID.toUUID(),
                name = name!!,
                nationalId = nationalId!!,
                zipCode = zipCode,
                country = country!!,
                city = city,
                address = address,
                active = !FALSE.equals(active),
            )
        }
    }.mapLeft { errors -> OrganizationCreationErrorList(errors) }
}