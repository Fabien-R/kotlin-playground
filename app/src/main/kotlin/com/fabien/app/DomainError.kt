package com.fabien.app

import io.ktor.http.*

sealed interface DomainError

sealed interface OrganizationIdentityError : DomainError
data class InseeError(val status: HttpStatusCode) : OrganizationIdentityError
data class InseeOtherError(val status: HttpStatusCode, val description: String) : OrganizationIdentityError
object InseeNotFound : OrganizationIdentityError
object MissingNationalIdOrSearchText : OrganizationIdentityError

sealed interface InvoiceExtractionError : DomainError
sealed class MindeeError(val status: HttpStatusCode = HttpStatusCode.InternalServerError, val description: String) : InvoiceExtractionError
data class MindeeUnAuthorizedError(val message: String) : MindeeError(description = message)
data class MindeeIOError(val message: String) : MindeeError(description = message)
data class MindeeOtherError(val message: String) : MindeeError(description = message)

class InseeException(val status: HttpStatusCode) : RuntimeException(status.description)
