package com.fabien

sealed interface DomainError

sealed interface OrganizationIdentityError : DomainError
data class InseeError(val statusCode: Int, val message: String) : OrganizationIdentityError
object InseeNotFound : OrganizationIdentityError
object MissingNationalIdOrSearchText : OrganizationIdentityError
