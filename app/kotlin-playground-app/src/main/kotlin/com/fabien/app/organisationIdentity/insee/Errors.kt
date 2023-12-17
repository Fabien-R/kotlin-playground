package com.fabien.app.organisationIdentity.insee

import arrow.core.Either
import com.fabien.domain.*
import com.fabien.domain.model.PaginatedOrganizations
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*

context(PipelineContext<Unit, ApplicationCall>)
suspend inline fun <reified A : Any> Either<OrganizationIdentityError, A>.respond(status: HttpStatusCode): Unit =
    when (this) {
        is Either.Left -> respond(value)
        is Either.Right -> call.respond(status, value)
    }

@Suppress("ComplexMethod")
suspend fun PipelineContext<Unit, ApplicationCall>.respond(error: OrganizationIdentityError): Unit =
    when (error) {
        // https://www.sirene.fr/static-resources/htm/codes_retour.html for all Insee return code
        is InseeError -> call.respond(HttpStatusCode.InternalServerError, "Our suppliers has respond with status ${error.statusCode} and fault ${error.description}")
        is InseeOtherError -> call.respond(HttpStatusCode.InternalServerError, "Our suppliers has respond with status ${error.statusCode}, fault ${error.description} and description ${error.description}")
        is InseeNotFound -> call.respond(HttpStatusCode.NotFound, PaginatedOrganizations(emptyList(), 0, 0))
        is MissingNationalIdOrSearchText -> call.respond(HttpStatusCode.UnprocessableEntity, "Require at least one of the nationalId or searchText parameters")
    }
