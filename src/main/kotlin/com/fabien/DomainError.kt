package com.fabien

import io.ktor.http.*

sealed interface DomainError

sealed interface OrganizationIdentityError : DomainError
data class InseeError(val status: HttpStatusCode) : OrganizationIdentityError
data class InseeOtherError(val status: HttpStatusCode, val description: String) : OrganizationIdentityError
object InseeNotFound : OrganizationIdentityError
object MissingNationalIdOrSearchText : OrganizationIdentityError

class InseeException(val status: HttpStatusCode) : RuntimeException(status.description)
