package com.fabien.app.invoiceExtraction

import com.fabien.app.invoiceExtraction.mindee.respond
import com.fabien.domain.services.InvoiceExtractionService
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*

fun Application.configureExtractionRouting(invoiceExtractionApi: InvoiceExtractionService) {
    routing {
        post("/extract") {
            call.receiveMultipart().forEachPart {
                if (it is PartData.FileItem) {
                    it.streamProvider().use { input ->
                        invoiceExtractionApi.fetchInvoiceExtraction(input).respond(HttpStatusCode.OK)
                    }
                }
            }
        }
    }
}
