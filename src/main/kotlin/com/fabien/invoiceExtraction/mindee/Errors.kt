package com.fabien.invoiceExtraction.mindee

import arrow.core.Either
import com.fabien.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*

context(PipelineContext<Unit, ApplicationCall>)
suspend inline fun <reified A : Any> Either<InvoiceExtractionError, A>.respond(status: HttpStatusCode): Unit =
    when (this) {
        is Either.Left -> respond(value)
        is Either.Right -> call.respond(status, value)
    }

@Suppress("ComplexMethod")
suspend fun PipelineContext<Unit, ApplicationCall>.respond(error: InvoiceExtractionError): Unit =
    when (error) {
        is MindeeError -> call.respond(error.status, "Our supplier is not able to extract the document")
    }.also {
        println("error: ${error.description}") // FIXME Logger
    }
