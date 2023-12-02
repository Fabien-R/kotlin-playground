package com.fabien.domain.handlers

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate
import com.fabien.domain.*
import com.fabien.domain.model.NewOrganization
import com.fabien.domain.model.Organization
import com.fabien.domain.repositories.OrganizationRepository

data class AddOrganizationCommand(
    val name: String?,
    val nationalId: String?,
    val zipCode: String?,
    val country: String?,
    val city: String?,
    val address: String?,
    val active: Boolean?,
)

fun interface AddOrganizationCommandHandler {
    suspend operator fun invoke(command: AddOrganizationCommand): Either<OrganizationError, Organization>
}

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
                    active = !java.lang.Boolean.FALSE.equals(active),
                )
            }
        }.mapLeft { errors ->
            OrganizationCreationErrorList(errors)
        }.bind().let { organization ->
            organizationRepository.save(organization).bind()
        }
    }
}
