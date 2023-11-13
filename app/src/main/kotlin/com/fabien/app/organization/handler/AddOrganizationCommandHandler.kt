package com.fabien.app.organization.handler

import arrow.core.NonEmptyList
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate
import com.fabien.app.*
import com.fabien.app.organization.AddOrganizationCommandHandler
import com.fabien.app.organization.NewOrganization
import com.fabien.app.organization.OrganizationRepository
import java.lang.Boolean.FALSE

fun addOrganizationCommandHandler(organizationRepository: OrganizationRepository) = AddOrganizationCommandHandler { addCommand ->
    either {
        val (name, nationalId, zipCode, country, city, address, active) = addCommand
        // TODO TRIM field name, zipcode, country, city, address
        // TODO remove space from nationalID
        either<NonEmptyList<OrganizationCreationError>, NewOrganization> {
            zipOrAccumulate(
                { ensure(!name.isNullOrEmpty()) { MissingName } },
                { ensure(!nationalId.isNullOrEmpty()) { MissingNationalId } },
                { ensure(!country.isNullOrEmpty()) { MissingCountry } },
                // TODO add national Id check
            ) { _, _, _ ->
                // TODO Upper case country, city
                NewOrganization(
                    name = name!!,
                    nationalId = nationalId!!,
                    zipCode = zipCode,
                    country = country!!,
                    city = city,
                    address = address,
                    active = !FALSE.equals(active),
                )
            }
        }.mapLeft { errors ->
            OrganizationCreationErrorList(errors)
        }.bind().let { organization ->
            organizationRepository.save(organization).bind()
        }
    }
}
