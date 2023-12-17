package com.fabien.domain

import java.util.*

sealed interface DomainError

sealed interface OrganizationIdentityError : DomainError
data class InseeError(val statusCode: Int, val description: String) : OrganizationIdentityError
data class InseeOtherError(val statusCode: Int, val description: String) : OrganizationIdentityError
object InseeNotFound : OrganizationIdentityError
object MissingNationalIdOrSearchText : OrganizationIdentityError

sealed interface OrganizationError : DomainError

data class OrganizationCreationErrorList(val errors: List<OrganizationCreationError>) : OrganizationError, List<OrganizationCreationError> by errors

sealed class OrganizationCreationError
object MissingName : OrganizationCreationError()
object MissingNationalId : OrganizationCreationError()
object MissingCountry : OrganizationCreationError()

sealed class OrganizationDBError : OrganizationError
data class OrganizationDuplication(val country: String, val nationalId: String) : OrganizationDBError()
data class OrganizationDBNotFound(val id: UUID) : OrganizationDBError()
data class OrganizationOtherDBErrors(val message: String?) : OrganizationDBError()

sealed interface InvoiceExtractionError : DomainError
sealed class MindeeError(val description: String) : InvoiceExtractionError
data class MindeeUnAuthorizedError(val message: String) : MindeeError(description = message)
data class MindeeIOError(val message: String) : MindeeError(description = message)
data class MindeeOtherError(val message: String) : MindeeError(description = message)

class InseeException(val statusCode: Int, val description: String) : RuntimeException(description)
