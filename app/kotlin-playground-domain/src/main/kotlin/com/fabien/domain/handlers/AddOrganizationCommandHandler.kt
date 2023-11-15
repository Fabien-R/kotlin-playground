package com.fabien.domain.handlers

import arrow.core.Either
import com.fabien.domain.OrganizationError
import com.fabien.domain.model.Organization

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
