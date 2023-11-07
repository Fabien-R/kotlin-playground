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
fun addOrganizationCommandHandler() = AddOrganizationCommandHandler { addCommand ->
    val (name, nationalId, zipCode, country, city, address, active) = addCommand
    either<NonEmptyList<OrganizationCreationError>, Organization> {
        zipOrAccumulate(
            { ensure(!name.isNullOrEmpty()) { MissingName } },
            { ensure(!nationalId.isNullOrEmpty()) { MissingNationalId } },
            { ensure(!country.isNullOrEmpty()) { MissingCountry } },
        ) { _, _, _ ->
            Organization(
                id = UUID.fromString(FAKE_UUID),
                name = name!!,
                nationalId = nationalId!!,
                zipCode = zipCode,
                country = country!!,
                city = city,
                address = address,
                active = FALSE.equals(active),
            )
        }
    }.mapLeft { errors -> OrganizationCreationErrorList(errors) }
}
