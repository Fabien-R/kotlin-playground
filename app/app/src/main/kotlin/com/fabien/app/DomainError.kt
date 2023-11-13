package com.fabien.app

import io.ktor.http.*
import java.util.*

sealed interface DomainError

sealed interface OrganizationIdentityError : DomainError
data class InseeError(val status: HttpStatusCode) : OrganizationIdentityError
data class InseeOtherError(val status: HttpStatusCode, val description: String) : OrganizationIdentityError
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
sealed class MindeeError(val status: HttpStatusCode = HttpStatusCode.InternalServerError, val description: String) : InvoiceExtractionError
data class MindeeUnAuthorizedError(val message: String) : MindeeError(description = message)
data class MindeeIOError(val message: String) : MindeeError(description = message)
data class MindeeOtherError(val message: String) : MindeeError(description = message)

class InseeException(val status: HttpStatusCode) : RuntimeException(status.description)
