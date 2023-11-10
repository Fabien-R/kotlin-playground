package com.fabien.app.organization

import arrow.core.Either
import com.fabien.app.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*

context(PipelineContext<Unit, ApplicationCall>)
suspend inline fun <reified A : Any> Either<OrganizationError, A>.respond(status: HttpStatusCode): Unit =
    when (this) {
        is Either.Left -> respond(value)
        is Either.Right -> call.respond(status, value)
    }

@Suppress("ComplexMethod")
suspend fun PipelineContext<Unit, ApplicationCall>.respond(error: OrganizationError): Unit =
    when (error) {
        is OrganizationCreationErrorList ->
            error.joinToString(separator = "|") {
                when (it) {
                    MissingName -> "missing name"
                    MissingNationalId -> "missing national id"
                    MissingCountry -> "missing country"
                }
            }.run {
                call.respond(HttpStatusCode.UnprocessableEntity, "Errors: $this")
            }

        is OrganizationDuplication -> call.respond(
            HttpStatusCode.Conflict,
            "Organization with country ${error.country} and national id ${error.nationalId} already exist",
        )

        is OrganizationDBNotFound -> call.respond(HttpStatusCode.NotFound)
        is OrganizationOtherDBErrors -> {
            println("error: ${error.message}") // FIXME Logger
        }
    }
